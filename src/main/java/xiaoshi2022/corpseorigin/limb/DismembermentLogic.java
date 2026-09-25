package xiaoshi2022.corpseorigin.limb;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/**
 * 截断判定。
 * <p>
 * 被动路径挂在 {@code ServerLivingEntityEvents.AFTER_DAMAGE}（伤害已结算）而不是 ALLOW_DAMAGE：
 * 黑金心脏已经在 ALLOW_DAMAGE 里拦截致命伤害，挂在同一事件里两边会互相干扰。
 * "什么样的攻击算截断来源"由 {@link LimbSeverRules} 决定，"长多快 / 拿什么当代价"由
 * {@link LimbRegenProfiles} 决定 —— 两者都是可注册的，加来源或加角色不用改这个类。
 * <p>
 * 技能（手刀、斩击、处决）走主动 API：{@link #severByHit}、{@link #severRandom}、
 * {@link #forceSever(ServerPlayer, int)}。
 */
public final class DismembermentLogic {

    /** 截断瞬间额外造成的断肢伤 */
    public static final float SEVER_EXTRA_DAMAGE = 2.0F;
    /** 新生脆弱期时长 */
    public static final int VULNERABLE_TICKS = 5 * 60 * 20;

    /**
     * 防重入：sever() 内部会补一次断肢伤，那次伤害同样会走 AFTER_DAMAGE。
     * 服务端是主线程单线程，用静态标记足够。
     */
    private static boolean severing;

    private DismembermentLogic() {
    }

    // ==================== 被动路径：被攻击触发 ====================

    /**
     * 受击结算后尝试截断。victim 必须是服务端玩家，且角色 / 尸兄身份都在门槛内。
     *
     * @param baseDamage  未穿甲的原始伤害（斧/剑挥出去的那一下有多重）
     * @param damageTaken 实际打进去的伤害
     */
    public static void tryDismember(ServerPlayer victim, DamageSource source,
                                    float baseDamage, float damageTaken) {
        if (severing) {
            return;   // 断肢补伤害引发的二次判定，直接放过
        }
        if (!canBeSevered(victim)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == victim) {
            return;
        }

        float hitPower = Math.max(baseDamage, damageTaken);
        LimbSeverRule rule = LimbSeverRules.firstMatch(victim, source, hitPower, damageTaken);
        if (rule == null) {
            return;
        }

        PlayerCorpseComponent comp = PlayerCorpseComponent.get(victim);
        LimbState state = comp.readLimbs();
        int slot = pickSlot(victim, attacker, source.getSourcePosition(), state);
        if (slot < 0) {
            return;
        }

        if (!rule.guaranteed()
                && victim.getRandom().nextFloat() >= rule.chance(victim, state, slot, hitPower)) {
            return;
        }

        sever(victim, slot, state, comp);
    }

    // ==================== 主动路径：技能 / 剧情 ====================

    /**
     * 技能用：按命中方位选部位，再掷一次概率。
     * <p>
     * 例（龙右·手刀）：
     * <pre>{@code
     * DismembermentLogic.severByHit(target, player, target.position().add(0, 1.2, 0), 0.8F);
     * }</pre>
     *
     * @param attacker 用于判断左右侧；可为 null（退化为随机侧）
     * @param hitPos   命中点（决定手 / 腿）；可为 null（随机）
     * @param chance   截断概率，1.0 = 必断
     */
    public static boolean severByHit(ServerPlayer victim, @Nullable Entity attacker,
                                     @Nullable Vec3 hitPos, float chance) {
        if (!canBeSevered(victim)) {
            return false;
        }
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(victim);
        LimbState state = comp.readLimbs();
        int slot = pickSlot(victim, attacker, hitPos, state);
        if (slot < 0) {
            return false;
        }
        if (chance < 1.0F && victim.getRandom().nextFloat() >= chance) {
            return false;
        }
        sever(victim, slot, state, comp);
        return true;
    }

    /** 技能用：随机断一个还完好的部位（爆炸、乱刀） */
    public static boolean severRandom(ServerPlayer victim, float chance) {
        if (!canBeSevered(victim)) {
            return false;
        }
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(victim);
        LimbState state = comp.readLimbs();
        int slot = LimbSlots.randomIntactSlot(state.mask(), victim.getRandom());
        if (slot < 0) {
            return false;
        }
        if (chance < 1.0F && victim.getRandom().nextFloat() >= chance) {
            return false;
        }
        sever(victim, slot, state, comp);
        return true;
    }

    /**
     * 指定部位必断，返回是否真的断了。
     * <p>
     * 这是<b>跳过身份门槛</b>的直通入口：调试指令、剧情演出（"这只手是导演要断的"）用它；
     * 走正常战斗逻辑请用 {@link #severByHit} / {@link #severRandom}，那两个会检查门槛。
     */
    public static boolean forceSever(ServerPlayer victim, int slot) {
        if (victim == null || victim.isDeadOrDying() || !victim.isAlive()) {
            return false;
        }
        if (slot < 0 || slot >= LimbSlots.COUNT) {
            return false;
        }
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(victim);
        LimbState state = comp.readLimbs();
        if (state.isSevered(slot)) {
            return false;
        }
        sever(victim, slot, state, comp);
        return true;
    }

    /** 门槛检查：没死、是服务端玩家、角色与尸兄身份都在范围内 */
    private static boolean canBeSevered(ServerPlayer victim) {
        if (victim == null || victim.isDeadOrDying() || !victim.isAlive()) {
            return false;
        }
        return LimbAccess.canDismember(victim);
    }

    // ==================== 部位选择 ====================

    /**
     * 选部位：先按攻击者相对受击者的左右侧定左右，再按命中高度定手 / 腿。
     * 选中的部位已断时换同一对的另一边，两边都断返回 -1。
     */
    private static int pickSlot(ServerPlayer victim, @Nullable Entity attacker,
                               @Nullable Vec3 hitPos, LimbState state) {
        boolean onRightSide;
        if (attacker != null) {
            float yaw = victim.getYRot() * Mth.DEG_TO_RAD;
            // 受击者右手方向（yaw=0 朝 +Z 时，右手指向 -X）
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            Vec3 toAttacker = attacker.position().subtract(victim.position());
            onRightSide = toAttacker.x * rightX + toAttacker.z * rightZ >= 0.0D;
        } else {
            onRightSide = victim.getRandom().nextBoolean();
        }

        boolean leg;
        if (hitPos != null) {
            leg = hitPos.y < victim.getY() + victim.getBbHeight() * 0.5D;
        } else {
            leg = victim.getRandom().nextBoolean();
        }

        // 头部：只对「尸王」开放 —— 他是不死髅体，脑袋掉了也能自己长回来；
        // 别的尸兄没有再生策略，断头就是永久残废，所以不给砍。
        // 判据是"从上往下劈中天灵盖"：攻击者得比受害者高半格以上（跳劈、从台阶上劈），
        // 且这一下落在头顶（高于身高的 85%）。
        // ⚠️ 近战伤害的 {@code getSourcePosition()} 是 null（只有爆炸/投掷物才带位置），
        // 所以这里不能只靠 hitPos，得退回用攻击者的眼睛高度来判高度。
        double strikeY = hitPos != null
                ? hitPos.y
                : (attacker != null ? attacker.getEyeY() : Double.NaN);
        boolean fromAbove = attacker != null && attacker.getY() >= victim.getY() + 0.5D;
        if (fromAbove
                && strikeY >= victim.getY() + victim.getBbHeight() * 0.85D
                && !state.isSevered(LimbSlots.HEAD)
                && LongYou.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(victim))) {
            return LimbSlots.HEAD;
        }

        int first = leg
                ? (onRightSide ? LimbSlots.RIGHT_LEG : LimbSlots.LEFT_LEG)
                : (onRightSide ? LimbSlots.RIGHT_ARM : LimbSlots.LEFT_ARM);
        int second = leg
                ? (onRightSide ? LimbSlots.LEFT_LEG : LimbSlots.RIGHT_LEG)
                : (onRightSide ? LimbSlots.LEFT_ARM : LimbSlots.RIGHT_ARM);

        if (!state.isSevered(first)) {
            return first;
        }
        if (!state.isSevered(second)) {
            return second;
        }
        return -1;
    }

    // ==================== 执行截断 ====================

    /** 真正断掉：写状态、补伤害、特效、同步 */
    private static void sever(ServerPlayer victim, int slot, LimbState old, PlayerCorpseComponent comp) {
        LimbRegenProfile profile = LimbRegenProfiles.forPlayer(victim);
        int total = profile.regrowTicks(victim, comp, slot);

        int[] regrow = old.regrowTicks().clone();
        int[] totals = old.totals().clone();
        int[] cooldowns = old.cooldowns().clone();
        regrow[slot] = total;
        totals[slot] = Math.max(0, total);
        cooldowns[slot] = VULNERABLE_TICKS;

        comp.writeLimbs(new LimbState(old.withSevered(slot), regrow, totals, cooldowns));

        // ★ 头被砍掉时，头盔跟着脑袋一起离开 —— 不然那顶头盔会挂在没脑袋的脖子上
        if (slot == LimbSlots.HEAD) {
            dropHeadArmor(victim);
        }

        // 立刻广播（含自己），客户端靠 mask 切到断肢模型
        CorpseNetwork.broadcastPlayerCorpseSync(victim);

        applyLimbEffects(victim, comp.readLimbs());

        // 断肢伤：guard 保证它不会再触发一次截断判定
        severing = true;
        try {
            victim.hurt(victim.level().damageSources().generic(), SEVER_EXTRA_DAMAGE);
        } finally {
            severing = false;
        }

        if (victim.level() instanceof ServerLevel level) {
            QiEffects.burst(level, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6D, victim.getZ(),
                    0xc0182a, 16, 0.4D);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.6F);
        }

        victim.sendOverlayMessage(Component.translatable(
                "limb.corpseorigin.severed", LimbSlots.DISPLAY_NAMES[slot]));
    }

    /**
     * 头被砍掉时把头盔摘下来：先塞回背包，背包塞不下就掉在脚下。
     * <p>
     * 不摘的话，那顶头盔会继续挂在"没有脑袋的脖子"上 —— 断肢期间盔甲本来就整身不画，
     * 但那只是渲染层的处理，物品还好端端地占着装备槽；这里让物品也跟着一起掉。
     */
    private static void dropHeadArmor(ServerPlayer victim) {
        ItemStack helmet = victim.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            return;
        }
        victim.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        // 先塞回背包；一个空位都没有时原版会自己把它丢在脚下（见 Inventory#placeItemBackInInventory）
        victim.getInventory().placeItemBackInInventory(helmet);
    }

    /**
     * 断肢期间的能力削弱。每 20 tick 由 {@link LimbRegenTickHandler} 续一次，所以这里用短时长。
     */
    public static void applyLimbEffects(ServerPlayer player, LimbState state) {
        boolean rightArm = state.isSevered(LimbSlots.RIGHT_ARM);
        boolean leftArm = state.isSevered(LimbSlots.LEFT_ARM);
        boolean rightLeg = state.isSevered(LimbSlots.RIGHT_LEG);
        boolean leftLeg = state.isSevered(LimbSlots.LEFT_LEG);

        if (state.isSevered(LimbSlots.HEAD)) {
            // 没脑袋：眼前一片黑（脖子以上都空了），直到脑袋长回来
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        }
        if (rightArm || leftArm) {
            // 独臂：挖掘疲劳 III（主手侧断掉时手也用不利索）
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 2, false, false, true));
        }
        if (rightLeg && leftLeg) {
            // 双腿：缓慢 IV，移速封顶
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3, false, false, true));
        } else if (rightLeg || leftLeg) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false, true));
        }
    }

    // ==================== 调试指令入口 ====================

    /** 立刻完成某个部位的再生，返回是否原本就断着 */
    public static boolean forceRegrow(ServerPlayer victim, int slot) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(victim);
        LimbState old = comp.readLimbs();
        if (!old.isSevered(slot)) {
            return false;
        }

        int[] regrow = old.regrowTicks().clone();
        int[] totals = old.totals().clone();
        int[] cooldowns = old.cooldowns().clone();
        regrow[slot] = LimbSlots.REGROW_PERMANENT;
        totals[slot] = 0;
        cooldowns[slot] = VULNERABLE_TICKS;

        comp.writeLimbs(new LimbState(old.withoutSevered(slot), regrow, totals, cooldowns));
        CorpseNetwork.broadcastPlayerCorpseSync(victim);
        return true;
    }

    /** 清空全部断肢状态（含永久断），返回是否原本有断肢 */
    public static boolean clearLimbs(ServerPlayer victim) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(victim);
        if (!comp.readLimbs().hasSevered()) {
            return false;
        }
        comp.clearLimbs();
        CorpseNetwork.broadcastPlayerCorpseSync(victim);
        return true;
    }
}
