package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天线宝宝盔甲的「吸食」通用处理器。
 * <p>
 * 把 {@link LifeDrainSuckSkill} 的核心逻辑从「只能玩家用」抽出来，
 * 改成任意 {@link LivingEntity}（玩家 / 穿戴套装的生物）都能触发。
 * <p>
 * 行为：抓住身边最近的目标 → 压制其移动并持续吸血（自身回血）→
 * 目标死亡 / 被拉开距离 / 自身受击时中断。
 * 吸食期间盔甲播 {@code absorb} 动画，触手转向目标。
 */
public final class EntityAntennaSuckHandler {

    private EntityAntennaSuckHandler() {}

    private static final double GRAB_RANGE = 8.0;
    private static final int DURATION = 120;
    private static final int DRAIN_INTERVAL = 6;
    private static final float DRAIN_DAMAGE = 4.0f;
    private static final float HEAL_RATIO = 0.8f;
    private static final double BREAK_DISTANCE = 9.5;

    private static final double SLOWNESS_CHANCE = 0.4;
    private static final int SLOWNESS_DURATION = 24;
    private static final int SLOWNESS_AMPLIFIER = 2;

    private static final int BUFF_DURATION = 100;
    private static final int SPECIAL_BLOOD_BUFF_DURATION = 300;
    private static final int SPECIAL_BLOOD_AMPLIFIER_BONUS = 1;

    /** 施术者 uuid → 正在进行的吸食 */
    private static final Map<UUID, Suck> ACTIVE = new ConcurrentHashMap<>();

    /**
     * 开始一次吸食。
     * <p>
     * 玩家由 {@link LifeDrainSuckSkill#onActivate} 主动调用；
     * 穿戴胸甲的生物在攻击命中时由事件处理器调用。
     *
     * @return true = 成功开始吸食；false = 没有可抓的目标 / 已经在吸
     */
    public static boolean start(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        if (ACTIVE.containsKey(caster.getUUID())) {
            return false;
        }

        LivingEntity target = findGrabbableTarget(caster, level);
        if (target == null) {
            return false;
        }

        ACTIVE.put(caster.getUUID(),
                new Suck(target, caster.tickCount + DURATION, caster.tickCount + DRAIN_INTERVAL));
        tryApplySlowness(target);

        // 告诉所有客户端"它在吸食谁"，盔甲据此播 absorb 并把触手转向目标
        CorpseNetwork.broadcastAntennaSuck(caster, target.getId(), DURATION);
        return true;
    }

    /** 服务端每 tick 推进所有进行中的吸食 */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Suck>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Suck> entry = iterator.next();
            LivingEntity caster = findCaster(server, entry.getKey());
            if (caster == null || !(caster.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            if (!tickOne(caster, level, entry.getValue())) {
                iterator.remove();
            }
        }
    }

    /** 按 uuid 找回施术者实体（玩家用 getPlayer，生物遍历全维度） */
    private static LivingEntity findCaster(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player;
        }
        // 生物：遍历所有维度找
        for (ServerLevel level : server.getAllLevels()) {
            LivingEntity e = level.getEntity(uuid) instanceof LivingEntity le ? le : null;
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** @return false 表示这次吸食到此为止 */
    private static boolean tickOne(LivingEntity caster, ServerLevel level, Suck suck) {
        LivingEntity target = suck.target;

        if (!caster.isAlive() || !target.isAlive() || target.isRemoved() || target.level() != level) {
            stop(caster, suck);
            return false;
        }
        if (caster.distanceTo(target) > BREAK_DISTANCE) {
            stop(caster, suck);
            return false;
        }
        // 自己挨打就松手
        if (caster.hurtTime > 0) {
            stop(caster, suck);
            return false;
        }

        // 血色粒子：从目标流回施术者
        Vec3 from = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 to = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);
        for (int i = 0; i < 4; i++) {
            double t = i / 4.0;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // 每隔一段时间吸一口
        if (caster.tickCount >= suck.nextDrain) {
            // 玩家走 playerAttack（保留击杀归属：经验/掉落），生物走 mobAttack
            target.hurtServer(level, caster instanceof ServerPlayer player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(caster), DRAIN_DAMAGE);
            caster.heal(DRAIN_DAMAGE * HEAL_RATIO);
            tryApplySlowness(target);
            applyBloodBuff(caster, target, suck);
            suck.nextDrain = caster.tickCount + DRAIN_INTERVAL;
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 0.8F);
        }

        if (caster.tickCount >= suck.until) {
            stop(caster, suck);
            return false;
        }
        return true;
    }

    /** 结束一次吸食：通知客户端撤掉 absorb 动画 */
    private static void stop(LivingEntity caster, Suck suck) {
        CorpseNetwork.broadcastAntennaSuck(caster, -1, 0);
    }

    private static void tryApplySlowness(LivingEntity target) {
        if (target.getRandom().nextDouble() >= SLOWNESS_CHANCE) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER, false, false, false));
    }

    /**
     * 吸食带来的强化：吸到"好血"就让施术者短暂变强。
     * 玩家给消息提示，生物只加效果。
     */
    private static void applyBloodBuff(LivingEntity caster, LivingEntity target, Suck suck) {
        boolean special = isSpecialBlood(target);
        int duration = special ? SPECIAL_BLOOD_BUFF_DURATION : BUFF_DURATION;
        int bonus = special ? SPECIAL_BLOOD_AMPLIFIER_BONUS : 0;

        caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, bonus, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, bonus, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, bonus, false, false, true));
    }

    /** 是否"特殊血液"：被吸的目标是本模组的角色玩家 */
    private static boolean isSpecialBlood(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        return character != null && !MortalCharacter.ID.equals(character.getId());
    }

    /** 找身边最近的目标 —— 不看方向 */
    private static LivingEntity findGrabbableTarget(LivingEntity caster, ServerLevel level) {
        Vec3 center = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);

        LivingEntity best = null;
        double bestDistance = GRAB_RANGE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(GRAB_RANGE),
                e -> e != caster && e.isAlive() && !(e instanceof Player p && p.isSpectator()))) {
            double distance = center.distanceTo(
                    candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    /** 该实体是否正在吸食 */
    public static boolean isSucking(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }

    /** 一次进行中的吸食 */
    private static final class Suck {
        private final LivingEntity target;
        private final int until;
        private int nextDrain;

        private Suck(LivingEntity target, int until, int nextDrain) {
            this.target = target;
            this.until = until;
            this.nextDrain = nextDrain;
        }
    }
}
