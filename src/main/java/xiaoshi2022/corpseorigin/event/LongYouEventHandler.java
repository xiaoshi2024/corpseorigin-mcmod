package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.effect.BYeffect;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.skill.longyou.ThunderPowerSkill;
import xiaoshi2022.corpseorigin.skill.longyou.ThunderStrikeHandler;
import xiaoshi2022.corpseorigin.skill.longyou.WaterPollutionSkill;

/**
 * 龙右专属事件处理。
 * <p>
 * 承载四件事：
 * <ul>
 *   <li>「雷电之力」（<b>主动开关</b>）：开着的时候近战命中附带雷伤与麻痹
 *       （{@link ThunderStrikeHandler#meleeZap}）；</li>
 *   <li>「尸水之源」的<b>地形</b>效果：开着的时候走过的水源被污染成尸水；</li>
 *   <li>「尸水之源」的<b>吸食</b>效果：开着的时候近战命中重伤目标，有概率把尸水灌进去感染对方
 *       （村民/玩家会变异成尸兄）—— 概率 + 血量门槛是为了不超模；</li>
 *   <li><b>吸食气血</b>：打「普通尸兄」时按伤害回血（尸王吸收同类气血的看家本事）。</li>
 * </ul>
 * 两个技能都要"该玩家是龙右 + 已学会"才生效；尸水之源还得自己按一下开启
 * （见 {@link WaterPollutionSkill}），代价是持续消耗饱食度。
 */
public final class LongYouEventHandler {

    /** 扫描间隔（tick）：走路时 4 次/秒足够跟上，不必每 tick 都扫 */
    private static final int SCAN_INTERVAL = 5;

    /**
     * 防重入：附雷本身会造成一次伤害，那次伤害同样会走 AFTER_DAMAGE。
     * 服务端主线程单线程，静态标记足够。
     */
    private static boolean zapping;

    /** 尸水之源·吸食感染：近战命中把尸水灌进去的概率（防止超模，不做成必中） */
    private static final float INFECT_CHANCE = 0.35F;
    /** 目标血量低于最大血量的这个比例才"吸得动"（还有大半管血时免疫） */
    private static final float INFECT_HEALTH_RATIO = 0.5F;

    /** 尸王吸食尸兄气血：按实际伤害的这个比例回血 */
    private static final float KIN_LIFESTEAL_RATIO = 0.5F;
    /** 单次回血上限（不然一道范围雷劈下去能一次吸一大口） */
    private static final float KIN_LIFESTEAL_MAX = 6.0F;

    private LongYouEventHandler() {
    }

    public static void register() {
        // 雷电之力（开关开启中）：龙右的近战命中附带额外雷伤与麻痹
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (zapping) {
                return;   // 附雷那一下自己引发的二次判定，放过
            }
            if (blocked || !(entity instanceof LivingEntity target)) {
                return;
            }
            if (!(source.getEntity() instanceof ServerPlayer caster)) {
                return;
            }
            // 只对"人打人"的近战生效：箭矢、投掷物这类 directEntity 不是本人的攻击不算
            if (source.getDirectEntity() != caster || target == caster) {
                return;
            }
            if (!isThunderActive(caster)) {
                return;
            }

            if (!xiaoshi2022.corpseorigin.character.InnerPowerManager.consume(caster, 2)) {
                ThunderPowerSkill.clearOnDisconnect(caster.getUUID());
                caster.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.long_you_event_handler.text_01"));
                return;
            }
            zapping = true;
            try {
                ThunderStrikeHandler.meleeZap(caster, target);
            } finally {
                zapping = false;
            }
        });

        // 尸水之源（开启中）：近战"吸食"重伤目标，有概率把尸水灌进去感染对方
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (zapping || blocked || !(entity instanceof LivingEntity target)) {
                return;
            }
            if (!(source.getEntity() instanceof ServerPlayer caster)
                    || source.getDirectEntity() != caster || target == caster) {
                return;
            }
            // 尸族自己人不感染（他们本来就是尸兄）
            if (ZombieKin.isZombieKin(target) || target.hasEffect(ModEffects.QIANS)) {
                return;
            }
            if (!isPollutionActive(caster)) {
                return;
            }
            // 血还厚就吸不动：得先把对方打残
            if (target.getHealth() > target.getMaxHealth() * INFECT_HEALTH_RATIO) {
                return;
            }
            // 概率感染：必中的话一爪子一个尸兄，太超模
            if (caster.getRandom().nextFloat() >= INFECT_CHANCE) {
                return;
            }

            BYeffect.applyInfection(target, (ServerLevel) caster.level(), caster.getUUID());
            infectFeedback((ServerLevel) caster.level(), target);
        });

        // 尸王吸食气血：龙右打"普通尸兄"时按伤害回血
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            // 附雷那一下不再单独回一次血，一次挥砍只吸一口
            if (zapping || blocked || damageTaken <= 0.0F) {
                return;
            }
            if (!(source.getEntity() instanceof ServerPlayer caster)) {
                return;
            }
            if (!(entity instanceof LivingEntity target) || target == caster) {
                return;
            }
            // 只吸"普通尸兄"（NPC）：尸兄玩家和其它生物都不算
            if (target instanceof Player || target.isAlliedTo(caster) || !ZombieKin.isZombieKin(target)
                    || target instanceof xiaoshi2022.corpseorigin.entity.CorpseAntEntity
                    || target instanceof xiaoshi2022.corpseorigin.entity.VampireBatEntity) {
                return;
            }
            // 高等级尸兄与指定角色伤害同类可积攒气血，直接吸血治疗仍是尸王天赋。
            String role = CharacterManager.getInstance().getPlayerCharacterId(caster);
            if (!xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.isEligible(caster)) {
                return;
            }
            xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.addCombat(caster,damageTaken);
            if (!LongYou.ID.equals(role) || caster.getHealth() >= caster.getMaxHealth()) {
                return;
            }

            float heal = Math.min(damageTaken * KIN_LIFESTEAL_RATIO, KIN_LIFESTEAL_MAX);
            caster.heal(heal);
            if (caster.level() instanceof ServerLevel level) {
                lifestealFeedback(level, caster, target);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!isPollutionActive(player)) {
                    continue;
                }
                // 用玩家自己的 id 错开相位，避免所有龙右挤在同一 tick 里扫
                int phase = player.tickCount + player.getId();

                // ① 代价：开着就一直掉饱食度；饿到见底自动中断
                if (phase % WaterPollutionSkill.HUNGER_INTERVAL == 0 && !drainHunger(player)) {
                    continue;
                }
                // ② 效果：脚边一小圈水源变尸水
                if (phase % SCAN_INTERVAL == 0) {
                    polluteAround(player);
                }
            }
        });

        CorpseOrigin.LOGGER.info("LongYou events registered");
    }

    /** 该玩家是不是"开着尸水之源的龙右" */
    private static boolean isPollutionActive(ServerPlayer player) {
        if (!LongYou.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        return WaterPollutionSkill.isEnabled(player.getUUID());
    }

    /** 该玩家是不是"开着雷电之力的龙右" */
    private static boolean isThunderActive(ServerPlayer player) {
        if (!LongYou.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        return ThunderPowerSkill.isEnabled(player.getUUID());
    }

    /**
     * 吸食气血的表现：一小串血色粒子从目标流回龙右。
     * <p>
     * 每次命中都会触发，所以刻意不加音效 —— 否则连着砍就是一路"咕嘟"声。
     */
    private static void lifestealFeedback(ServerLevel level, LivingEntity caster, LivingEntity target) {
        Vec3 from = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 to = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);
        for (int i = 1; i <= 3; i++) {
            double t = i / 4.0;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** 感染成功时的表现：一蓬尸水绿雾 + 咕嘟声 */
    private static void infectFeedback(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        level.sendParticles(ParticleTypes.WITCH, x, y, z, 18, 0.4, 0.5, 0.4, 0.02);
        level.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 10, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, x, y, z, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    /**
     * 扣饱食度。
     *
     * @return false = 已经饿得扣不动了，技能被自动关闭
     */
    private static boolean drainHunger(ServerPlayer player) {
        if (player.getFoodData().getFoodLevel() <= 0) {
            WaterPollutionSkill.disable(player);
            return false;
        }
        player.causeFoodExhaustion(WaterPollutionSkill.HUNGER_EXHAUSTION);
        return true;
    }

    /** 扫玩家脚下一小圈，把普通水源换成尸水源 */
    private static void polluteAround(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int changed = 0;

        int radius = WaterPollutionSkill.RADIUS;
        int vertical = WaterPollutionSkill.VERTICAL_RADIUS;

        for (int dx = -radius; dx <= radius && changed < WaterPollutionSkill.MAX_PER_SCAN; dx++) {
            for (int dz = -radius; dz <= radius && changed < WaterPollutionSkill.MAX_PER_SCAN; dz++) {
                for (int dy = -vertical; dy <= vertical && changed < WaterPollutionSkill.MAX_PER_SCAN; dy++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (polluteAt(level, pos)) {
                        changed++;
                    }
                }
            }
        }
    }

    /**
     * 把这一格的水源换成尸水源。
     *
     * @return true = 这格真的被污染了
     */
    private static boolean polluteAt(ServerLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);

        // 只污染"水源"：流动水不用管，它上游的源被换掉之后自己会重新流成尸水
        if (!fluid.isSource()) {
            return false;
        }

        Fluid type = fluid.getType();
        if (type != Fluids.WATER && type != Fluids.FLOWING_WATER) {
            return false;   // 已经是尸水，或者是别的流体，别乱动
        }

        level.setBlock(pos, ModFluids.INFECTED_WATER.defaultFluidState().createLegacyBlock(),
                Block.UPDATE_ALL);
        return true;
    }
}
