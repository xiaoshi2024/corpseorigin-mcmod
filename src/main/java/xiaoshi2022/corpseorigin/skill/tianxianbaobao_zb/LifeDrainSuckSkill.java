package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天线宝宝尸兄·吸食——近战抓住玩家持续吸血，可用打断技解救。
 * <p>
 * 主动技能，无冷却（0 ticks）。
 * <p>
 * 实装效果：抓住身边<b>任意方向</b>最近的目标 → 压制其移动并持续吸血（自身回血）→
 * 目标死亡 / 被拉开距离 / 自身受击时中断。
 * <p>
 * 吸食期间盔甲播 {@code absorb} 动画，并靠动画里的 {@code query.target_*_rotation}
 * 把触手转向目标：状态与目标实体 id 由 {@link CorpseNetwork#broadcastAntennaSuck}
 * 同步给所有客户端，渲染时读它算朝向。
 */
public class LifeDrainSuckSkill extends AbstractSkill {

    public static final String PATH = "life_drain_suck";

    /**
     * 起手抓取距离（不分方向：前后左右上下都算）。
     * <p>
     * 取 6 格是和动画对齐的：{@code absorb} 里 {@code bone6} 的 Y 缩放拉到 5.76，把上面那串骨骼
     * 推出去大约 6 格，所以判定给到这个距离，视觉上"伸出去够到了"才对得上。
     */
    private static final double GRAB_RANGE = 6.0;
    /** 单次吸食持续时长（tick） */
    private static final int DURATION = 60;
    /** 吸一口的间隔与数值 */
    private static final int DRAIN_INTERVAL = 10;
    private static final float DRAIN_DAMAGE = 2.0f;
    /** 吸血转回自身的比例 */
    private static final float HEAL_RATIO = 0.5f;
    /** 目标拉开到这个距离就算挣脱（要比抓取距离大一截，否则刚抓住就断） */
    private static final double BREAK_DISTANCE = 7.5;

    /** 施术者 uuid → 正在进行的吸食 */
    private static final Map<UUID, Suck> ACTIVE = new ConcurrentHashMap<>();

    public LifeDrainSuckSkill() {
        super(PATH, SkillType.COMBAT, 0);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        LivingEntity target = findGrabbableTarget(player, level);
        if (target == null) {
            player.sendSystemMessage(Component.translatable(msgKey("no_target")));
            return;
        }

        ACTIVE.put(player.getUUID(),
                new Suck(target, player.tickCount + DURATION, player.tickCount + DRAIN_INTERVAL));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION, 4, false, false, false));

        player.sendSystemMessage(Component.translatable(msgKey("grab"), target.getName()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 0.6F);

        // 告诉所有客户端"他在吸食谁"，盔甲据此播 absorb 并把触手转向目标
        CorpseNetwork.broadcastAntennaSuck(player, target.getId(), DURATION);
    }

    /** 服务端每 tick 推进所有进行中的吸食（由 {@code ServerEvents} 调用） */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Suck>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Suck> entry = iterator.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null || !(caster.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            if (!tickOne(caster, level, entry.getValue())) {
                iterator.remove();
            }
        }
    }

    /** @return false 表示这次吸食到此为止 */
    private static boolean tickOne(ServerPlayer caster, ServerLevel level, Suck suck) {
        LivingEntity target = suck.target;

        if (!caster.isAlive() || !target.isAlive() || target.isRemoved() || target.level() != level) {
            stop(caster, suck, "loose");
            return false;
        }
        if (caster.distanceTo(target) > BREAK_DISTANCE) {
            stop(caster, suck, "loose");
            return false;
        }
        // 自己挨打就松手——这就是"可用打断技解救"的判定入口
        if (caster.hurtTime > 0) {
            stop(caster, suck, "interrupted");
            return false;
        }

        // 抓住：持续压制目标移动
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 4, false, false, false));

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
            target.hurtServer(level, level.damageSources().playerAttack(caster), DRAIN_DAMAGE);
            caster.heal(DRAIN_DAMAGE * HEAL_RATIO);
            suck.nextDrain = caster.tickCount + DRAIN_INTERVAL;
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 0.8F);
        }

        if (caster.tickCount >= suck.until) {
            stop(caster, suck, null);
            return false;
        }
        return true;
    }

    /** 结束一次吸食：通知客户端撤掉 absorb 动画（reasonKey 非空时顺带提示一下） */
    private static void stop(ServerPlayer caster, Suck suck, String reasonKey) {
        if (reasonKey != null) {
            caster.sendSystemMessage(Component.translatable(msgKey(reasonKey), suck.target.getName()));
        }
        CorpseNetwork.broadcastAntennaSuck(caster, -1, 0);
    }

    /**
     * 找身边<b>最近</b>的目标 —— 不看方向。
     * <p>
     * 触手是靠动画里的 {@code query.target_*_rotation} 自己转过去对准目标的，
     * 所以这里不需要"必须正对"的前方锥形判定：前后左右、头上脚下都能抓。
     */
    private static LivingEntity findGrabbableTarget(ServerPlayer player, ServerLevel level) {
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);

        LivingEntity best = null;
        double bestDistance = GRAB_RANGE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(GRAB_RANGE),
                e -> e != player && e.isAlive())) {
            double distance = center.distanceTo(
                    candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
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
