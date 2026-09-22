package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 尸王次声波 —— 共振频率 + 内力判定 + 操控尸兄。
 * <p>
 * 三条效果同源，都围绕施术者身边 {@value #FIELD_RADIUS} 格的声场：
 * <ol>
 *   <li><b>共振解体</b>：箭矢 / 弩矢 / 三叉戟飞行时各有固有共振频率，
 *       次声波频率从 {@value #FREQ_START}Hz 扫到 {@value #FREQ_END}Hz，
 *       两者相差小于 {@value #RESONANCE_THRESHOLD}Hz 时引发共振，投掷物当场解体
 *       （<b>只解体飞行中的投掷物，不会损坏任何人的手持武器</b>）；</li>
 *   <li><b>晕厥 / 削弱</b>：对玩家与生物，按"有没有内力、内力够不够"三档判定 ——
 *       当前内力 ≥ {@value #INNER_POWER_IMMUNE} 直接硬抗（免疫）；有内力但不足则被抽掉一部分内力、
 *       只受削弱；完全没有内力的直接晕厥并受伤；</li>
 *   <li><b>操控尸兄</b>：范围内的尸族生物被打上 {@value #CONTROL_TAG} 标签，
 *       声场持续期间跟随施术者、并扑向施术者当前攻击的目标。</li>
 * </ol>
 * ⚠️ 施术者类型故意写成 {@link LivingEntity} 而不是 {@code ServerPlayer}：
 * 次声波认的是"龙右的身体"而不是账号 —— 玩家把意识转移进龙右克隆身体后照样能操控尸兄，
 * 将来让龙右分身实体自己放这招也不用改这里。
 */
public final class InfrasoundFieldHandler {

    private InfrasoundFieldHandler() {
    }

    /** 声场半径（格） */
    public static final double FIELD_RADIUS = 12.0;
    /** 声场持续时长（tick）：3 秒 */
    public static final int DURATION = 60;

    /** 被操控的尸兄身上的标签（技能结束后移除） */
    public static final String CONTROL_TAG = "corpseorigin:infrasound_controlled";

    // ==================== 频率 ====================

    /** 次声波起始频率（Hz） */
    private static final float FREQ_START = 14.0F;
    /** 次声波终止频率（Hz）：声场从低频扫到高频，正好扫过一遍 */
    private static final float FREQ_END = 30.0F;
    /** 共振阈值（Hz）：|次声波频率 - 投掷物固有频率| < 阈值 → 引发共振 */
    private static final float RESONANCE_THRESHOLD = 1.2F;
    /** 共振判定间隔（tick） */
    private static final int SCAN_INTERVAL = 2;

    /** 各类投掷物的固有共振频率（Hz） */
    private static final float FREQ_ARROW = 18.0F;
    private static final float FREQ_SPECTRAL_ARROW = 24.0F;
    private static final float FREQ_TRIDENT = 28.0F;
    /** 其他没列出的投掷物 */
    private static final float FREQ_DEFAULT = 21.0F;

    // ==================== 内力三档 ====================

    /** 当前内力 ≥ 这个数就能硬抗次声波（免疫） */
    private static final int INNER_POWER_IMMUNE = 60;
    /** 有内力但不够：被抽走这么多内力，换来"只是削弱而不是晕厥" */
    private static final int INNER_POWER_DRAIN = 20;

    /** 无内力：晕厥 + 伤害 */
    private static final int STUN_DURATION = 100;
    private static final float STUN_DAMAGE = 6.0F;
    /** 内力不足：削弱 + 少量伤害 */
    private static final int WEAKEN_DURATION = 40;
    private static final float WEAKEN_DAMAGE = 3.0F;

    /** 施术者 uuid → 正在持续的声场 */
    private static final Map<UUID, Field> ACTIVE = new ConcurrentHashMap<>();

    // ==================== 起手 ====================

    /**
     * 释放次声波。
     *
     * @return true = 成功展开声场
     */
    public static boolean start(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        if (ACTIVE.containsKey(caster.getUUID())) {
            return false;
        }

        Field field = new Field(caster.tickCount, caster.tickCount + DURATION);
        ACTIVE.put(caster.getUUID(), field);

        // 起手：脉冲判定一次（晕厥 / 免疫 / 操控尸兄），并给出表现
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 0.7F);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.5, caster.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        pulse(caster, level, field);
        return true;
    }

    // ==================== 每 tick 推进 ====================

    /** 服务端每 tick 推进所有进行中的声场 */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Field>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Field> entry = iterator.next();
            Field field = entry.getValue();

            LivingEntity caster = findCaster(server, entry.getKey());
            if (caster == null || !(caster.level() instanceof ServerLevel level)) {
                releaseControlled(server, field);
                iterator.remove();
                continue;
            }
            if (caster.tickCount >= field.until) {
                releaseControlled(server, field);
                iterator.remove();
                continue;
            }

            // 频率扫描 → 共振解体
            if (caster.tickCount % SCAN_INTERVAL == 0) {
                float frequency = frequencyAt(caster.tickCount - field.startTick);
                shatterResonant(level, caster, frequency);
            }
            // 声场期间持续"下令"：跟随施术者、扑向他当前攻击的目标
            if (caster.tickCount % 10 == 0) {
                orderControlled(level, caster, field);
            }
        }
    }

    /** 当前扫到的次声波频率（Hz） */
    private static float frequencyAt(int elapsed) {
        float progress = Math.min(1.0F, elapsed / (float) DURATION);
        return FREQ_START + (FREQ_END - FREQ_START) * progress;
    }

    /** 这类投掷物的固有共振频率（Hz） */
    private static float resonanceOf(AbstractArrow arrow) {
        if (arrow instanceof SpectralArrow) {
            return FREQ_SPECTRAL_ARROW;
        }
        if (arrow instanceof ThrownTrident) {
            return FREQ_TRIDENT;
        }
        if (arrow instanceof Arrow) {
            return FREQ_ARROW;
        }
        return FREQ_DEFAULT;
    }

    /** 频率匹配的飞行投掷物 → 引发共振、当场解体 */
    private static void shatterResonant(ServerLevel level, LivingEntity caster, float frequency) {
        AABB area = caster.getBoundingBox().inflate(FIELD_RADIUS);
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, area)) {
            if (arrow.isRemoved()) {
                continue;
            }
            if (Math.abs(frequency - resonanceOf(arrow)) >= RESONANCE_THRESHOLD) {
                continue;
            }
            Vec3 pos = arrow.position();
            level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 8, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.0F, 1.6F);
            // 只有飞行中的投掷物会被震散：手里拿的武器、地上的掉落物都不受影响
            arrow.discard();
        }
    }

    // ==================== 起手脉冲：内战判定 ====================

    /** 起手那一下的判定：操控尸兄 / 晕厥 / 免疫 */
    private static void pulse(LivingEntity caster, ServerLevel level, Field field) {
        AABB area = caster.getBoundingBox().inflate(FIELD_RADIUS);
        // 克隆分身放招时不震自己的本体：分身是替本体办事的，不该反过来把主人震晕
        UUID ownerUuid = caster instanceof CloneAvatarEntity avatar ? avatar.getOwnerUuid() : null;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive() && !e.isSpectator()
                        && (ownerUuid == null || !ownerUuid.equals(e.getUUID())));

        for (LivingEntity target : targets) {
            // ① 尸族生物 → 操控，不伤害
            if (target instanceof Mob mob && ZombieKin.isZombieKin(target)) {
                mob.addTag(CONTROL_TAG);
                field.controlled.add(mob.getUUID());
                continue;
            }
            // ② 其他人形/生物 → 内力判定
            applyAgainstInnerPower(caster, level, target);
        }
    }

    /**
     * 内力三档判定。
     * <ul>
     *   <li>无内力（生物、无内力角色玩家）→ 直接晕厥 + 伤害</li>
     *   <li>有内力但当前值 < {@value #INNER_POWER_IMMUNE} → 抽掉一部分内力，只被削弱</li>
     *   <li>当前内力足够 → 免疫</li>
     * </ul>
     */
    private static void applyAgainstInnerPower(LivingEntity caster, ServerLevel level, LivingEntity target) {
        if (target instanceof ServerPlayer player && InnerPowerManager.getMaxInnerPower(player) > 0) {
            int power = InnerPowerManager.getInnerPower(player);
            if (power >= INNER_POWER_IMMUNE) {
                player.sendOverlayMessage(
                        Component.translatable("skill.corpseorigin.corpse_king_infrasound.resisted"));
                return;
            }
            // 内力不够硬抗：抽掉一部分（可能抽干），换来"只是被削弱"
            InnerPowerManager.set(player, Math.max(0, power - INNER_POWER_DRAIN));
            player.sendOverlayMessage(
                    Component.translatable("skill.corpseorigin.corpse_king_infrasound.overwhelmed"));
            hurt(caster, level, target, WEAKEN_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WEAKEN_DURATION, 2, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKEN_DURATION, 1, false, false, true));
            return;
        }

        // 无内力：直接晕厥
        hurt(caster, level, target, STUN_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUN_DURATION, 5, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_DURATION, 2, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, STUN_DURATION, 1, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STUN_DURATION / 2, 0, false, false, true));
    }

    /** 玩家施术算玩家击杀（保留掉落/经验归属），其他施术者算生物攻击 */
    private static void hurt(LivingEntity caster, ServerLevel level, LivingEntity target, float damage) {
        target.hurtServer(level, caster instanceof ServerPlayer player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(caster), damage);
    }

    // ==================== 操控尸兄 ====================

    /** 让被操控的尸兄跟随施术者，并扑向他当前攻击的目标 */
    private static void orderControlled(ServerLevel level, LivingEntity caster, Field field) {
        LivingEntity casterTarget = designatedTarget(caster);
        // 目标不合法就不下令攻击：死了 / 就是施术者自己 / 是分身的主人（哪有让自己的打手咬主人的）
        if (casterTarget != null && (!casterTarget.isAlive() || casterTarget == caster
                || caster instanceof CloneAvatarEntity avatar
                && casterTarget.getUUID().equals(avatar.getOwnerUuid()))) {
            casterTarget = null;
        }

        for (UUID uuid : field.controlled) {
            if (!(level.getEntity(uuid) instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (casterTarget != null && casterTarget != mob) {
                mob.setTarget(casterTarget);
                mob.getNavigation().moveTo(casterTarget, 1.2D);
            } else {
                // 没有指定目标就单纯跟着尸王走
                mob.setTarget(null);
                mob.getNavigation().moveTo(caster, 1.0D);
            }
        }
    }

    /**
     * 施术者"指定的目标"。
     * <p>
     * 生物施术者（例如龙右分身）用当前 AI 目标；玩家没有 {@code getTarget()}，
     * 用最近打过的那个 —— 也就是"我正打谁，你们就打谁"。
     */
    private static LivingEntity designatedTarget(LivingEntity caster) {
        if (caster instanceof Mob mob && mob.getTarget() != null) {
            return mob.getTarget();
        }
        return caster.getLastHurtMob();
    }

    /** 声场是否还在持续（同一个施术者不会同时开两场） */
    public static boolean isActive(UUID casterUuid) {
        return ACTIVE.containsKey(casterUuid);
    }

    /** 声场结束：摘掉标签（实体可能已经死了/卸载了，逐个按 uuid 找） */
    private static void releaseControlled(MinecraftServer server, Field field) {
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID uuid : field.controlled) {
                if (level.getEntity(uuid) instanceof Entity entity) {
                    entity.removeTag(CONTROL_TAG);
                }
            }
        }
        field.controlled.clear();
    }

    /** 按 uuid 找回施术者（玩家查在线列表，生物遍历各维度） */
    private static LivingEntity findCaster(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /** 一次进行中的声场 */
    private static final class Field {
        private final int startTick;
        private final int until;
        /** 被操控的尸兄 */
        private final List<UUID> controlled = new ArrayList<>();

        private Field(int startTick, int until) {
            this.startTick = startTick;
            this.until = until;
        }
    }
}
