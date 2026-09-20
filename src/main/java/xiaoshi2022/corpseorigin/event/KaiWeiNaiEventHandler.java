package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.kaiweinai.ChrysanthemumShieldSkill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开胃奶专属事件处理。
 * <p>
 * <b>一、菊花盾的格挡与箭矢反弹</b><br>
 * 判定与动画是分开的：动画窗口由技能自己广播（{@link ChrysanthemumShieldSkill#onActivate}
 * → {@code CorpseNetwork.broadcastNiunaiParry}），这里只认窗口本身：
 * <ul>
 *   <li><b>正面伤害</b>：盾窗口内整下挡掉（是格挡，不是减伤，也不是无敌帧）；</li>
 *   <li><b>迎面箭矢</b>：{@link AbstractArrow}（弓 / 弩 / 三叉戟）会被原样弹回去，
 *       并认这具身体为新的发射者 —— 所以能反过来伤到原射手，正是"反弹箭矢类似于盾牌"。</li>
 * </ul>
 * 判定用"是不是开胃奶 + 在不在窗口内"，和技能入口（只有开胃奶的技能列表里有这招）一致。
 * <p>
 * <b>二、黑骑士体质：致命一击不死，改为拦腰斩断</b><br>
 * 挂着 {@code ALLOW_DEATH}：真正会死的那一下被拦下，改走 {@link #startLinking} ——
 * 把血补回 {@link #LINK_HEALTH} 并广播一条表现窗口，客户端把模型整身换成
 * {@code niunai_link_player} 并播 {@code broken_off}（拦腰斩断）→ 保持 1 分钟 → {@code link}（接回）。
 * <p>
 * ⚠️ 他<b>不是无敌的</b>：这一下之后只留 {@link #LINK_HEALTH} 点血，残血期间再挨攻击照常死，
 * 只是那次死会顺手把腰斩表现清掉（见下面 AFTER_DEATH 那条）。
 * 窗口本身也不带任何减伤 / 免死 —— 它纯粹是表现。
 */
public final class KaiWeiNaiEventHandler {

    /** 拦腰斩断之后剩下的血量（半颗心）：只够活下来，残血再挨一下就会死 */
    private static final float LINK_HEALTH = 1.0F;

    /**
     * 「拦腰斩断」的结束时刻：玩家 uuid → 世界的 gameTime。
     * <p>
     * 同天线格挡 / 菊花盾：存的是<b>世界 gameTime</b>而不是 {@code player.tickCount} ——
     * 玩家重生后 tickCount 会归零，用 tickCount 会留下一段时间的"假腰斩"。
     */
    private static final Map<UUID, Long> LINK_UNTIL = new ConcurrentHashMap<>();

    private KaiWeiNaiEventHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            if (!hasShield(player) || !isFrontal(player, source)) {
                return true;
            }

            // 迎面飞来的箭矢 / 弩矢 / 三叉戟 → 原样弹回去
            if (source.getDirectEntity() instanceof AbstractArrow arrow && !arrow.isRemoved()) {
                reflect(player, arrow);
            }

            playBlockFeedback(player);
            // 整下挡掉：返回 false 直接取消这份伤害，不做任何重打（所以也不涉及递归）
            return false;
        });

        // ★ 黑骑士体质：致命一击不死，改为拦腰斩断
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            // /kill、虚空这类"绕过无敌"的伤害照常生效 —— 否则开胃奶就成了杀不死的，
            // 管理员连调试都做不了
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return true;
            }
            if (!KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
                return true;
            }

            if (isLinking(player)) {
                // 已经在腰斩中：★ 不再免死 —— 他就是残血（LINK_HEALTH）状态，
                // 这一下照样要他的命；真死了的话 AFTER_DEATH 那条会把腰斩表现清掉。
                return true;
            }

            startLinking(player);
            return false;   // 拦下这次死亡
        });

        // 真的死了（残血期间又挨了一下 / /kill / 虚空）：把腰斩状态清掉，
        // 否则客户端会一直挂着那具半截模型，直到窗口自己走完
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && LINK_UNTIL.remove(player.getUUID()) != null) {
                CorpseNetwork.broadcastNiunaiLink(player, 0);
            }
        });

        CorpseOrigin.LOGGER.info("KaiWeiNai events registered");
    }

    // ==================== 黑骑士体质：拦腰斩断 ====================

    /**
     * 开始一次拦腰斩断：记录窗口、把血补回来（致命一击已经扣到 0 以下了）、广播表现。
     * <p>
     * ⚠️ 补的血只够"活下来"（{@link #LINK_HEALTH}），不给他任何减伤 / 免死 ——
     * 残血期间再挨攻击就照常死。
     */
    private static void startLinking(ServerPlayer player) {
        LINK_UNTIL.put(player.getUUID(),
                player.level().getGameTime() + KaiWeiNai.NIUNAI_LINK_TOTAL_TICKS);

        // ★ 必须先补血再返回 false：事件是在 die() 开头拦下的，此时血量已经 ≤ 0，
        //   不补的话同一 tick 内还会被判定成"死亡中"，下一帧照样死。
        player.setHealth(LINK_HEALTH);

        player.sendOverlayMessage(Component.translatable(
                "corpseorigin.kaiweinai.link.trigger", KaiWeiNai.NIUNAI_LINK_HOLD_TICKS / 20));

        // 表现：整身换成 niunai_link_player，窗口覆盖"断开 + 保持 + 接回"整个过程
        CorpseNetwork.broadcastNiunaiLink(player, KaiWeiNai.NIUNAI_LINK_TOTAL_TICKS);
    }

    /** 这位开胃奶现在是否正处于「拦腰斩断」窗口内（顺带清掉过期项） */
    private static boolean isLinking(ServerPlayer player) {
        Long until = LINK_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            LINK_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /** 该玩家现在是不是"开胃奶"且处于菊花盾窗口内 */
    private static boolean hasShield(ServerPlayer player) {
        if (!KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        // 战斗中每一下都被调一次，所以放在角色判定之后（getPlayerCharacterId 更便宜）
        return ChrysanthemumShieldSkill.isShielding(player);
    }

    /**
     * 这记伤害是不是从<b>正面</b>来的（盾只护正面，背后挨打照样吃伤害）。
     * <ul>
     *   <li><b>投掷物</b>：命中那一刻它已经贴到身上了，用位置算不出方向 —— 改用"它朝哪飞"判断：
     *       迎面飞来的速度方向与我的朝向往相反；速度取不到时保守放行，别让盾漏；</li>
     *   <li><b>近战 / 其他直接来源</b>：用攻击者相对我的方位；</li>
     *   <li><b>没有直接来源</b>（摔落 / 着火 / 中毒这类环境伤害）：不吃盾。</li>
     * </ul>
     */
    private static boolean isFrontal(ServerPlayer player, DamageSource source) {
        Vec3 look = player.getLookAngle();
        Entity direct = source.getDirectEntity();

        if (direct instanceof Projectile projectile) {
            Vec3 motion = projectile.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-6) {
                return motion.normalize().dot(look) < 0.0;
            }
            return true;
        }

        if (direct != null) {
            Vec3 toSource = direct.position().subtract(player.position());
            if (toSource.lengthSqr() > 1.0E-6) {
                return look.dot(toSource.normalize()) > 0.0;
            }
        }
        return false;
    }

    /**
     * 把箭矢原样弹回去。
     * <p>
     * 掉头之后略微加速：按原速掉头的话，重力会很快把它拉到地上，看着不像"弹回去"。
     * 同时把发射者改成这具身体 —— 弹回去的箭才算我们的战果，也不会再判定打到自己。
     */
    private static void reflect(ServerPlayer player, AbstractArrow arrow) {
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(-1.15));
        arrow.setOwner(player);
        // 速度由服务端权威：不标这一下，客户端看不到这次掉头（同 APSSubSkills 的做法）
        arrow.hurtMarked = true;
    }

    /**
     * 挡下之后的反馈：一声闷响。
     * <p>
     * 盾张开的动画不在这里发 —— 它由技能开盾时广播的那条窗口覆盖整个持续时间，
     * 挨打时再发一遍只会把已经张开的花瓣每帧拽回第 0 帧重播。
     */
    private static void playBlockFeedback(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.2F);
    }
}
