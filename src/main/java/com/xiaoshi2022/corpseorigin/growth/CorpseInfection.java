package com.xiaoshi2022.corpseorigin.growth;

import com.xiaoshi2022.corpseorigin.config.CorpseOriginServerConfig;
import com.xiaoshi2022.corpseorigin.effect.BYeffect;
import com.xiaoshi2022.corpseorigin.entity.HybridZombie;
import com.xiaoshi2022.corpseorigin.entity.ZombieKin;
import com.xiaoshi2022.corpseorigin.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractFish;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 感染触发链路入口（1.20.1 Forge 版本）。
 * <p>
 * 与 26.2 Fabric 版本的差异：ServerLivingEntityEvents → Forge LivingHurtEvent。
 * 事件注册在 {@link com.xiaoshi2022.corpseorigin.event.ModEvents}。
 * <p>
 * 尸水浸泡规则（替代旧的"每秒 30% 概率直接感染"）：
 * <ul>
 *   <li>鱼类（{@link AbstractFish}）：接触尸水立即感染；</li>
 *   <li>其它生物：连续浸泡达到 {@code submergeSeconds}（默认 15 秒）后感染；</li>
 *   <li>离开水面超过 0.5 秒，浸泡计时重置，避免"反复沾水累计"。</li>
 * </ul>
 */
public final class CorpseInfection {

    private static final Map<LivingEntity, WaterExposure> WATER_EXPOSURE = new WeakHashMap<>();

    /** 离开尸水超过这个 tick 数就重新计时（连续浸泡才算）。 */
    private static final long EXPOSURE_GAP_TICKS = 10L;

    private CorpseInfection() {
    }

    /** Forge LivingHurtEvent 处理器：每次直接攻击命中，尸族按概率上感染。 */
    public static void onLivingHurt(LivingEntity target, LivingEntity source, float amount, boolean blocked) {
        if (blocked || amount <= 0 || source == null || target == null) return;
        if (!(source instanceof Mob attacker) || !ZombieKin.isZombieKin(attacker)) return;
        // 半尸兄刚感染的 Mob 也算尸族，可以继续传播
        tryInfectMob(attacker, target);
    }

    /** 尸族直接攻击 Mob：按概率上感染。 */
    public static void tryInfectMob(Mob attacker, LivingEntity target) {
        if (!(target instanceof Mob) || !BYeffect.canInfect(target) || target.hasEffect(ModEffects.QIANS.get())
                || HybridZombie.isHybrid(attacker)
                || !ZombieKin.isZombieKin(attacker)
                || !ZombieKin.canAttack(attacker, target)
                || attacker.isAlliedTo(target)
                || !(target.level() instanceof ServerLevel level)) return;
        // 默认 40% 几率，未来可接入配置
        if (attacker.getRandom().nextDouble() < 0.4)
            BYeffect.applyInfection(target, level, attacker.getUUID());
    }

    /** 尸水接触入口：由 InfectedWaterBlock.entityInside 每 tick 调用。 */
    public static void touchInfectedWater(LivingEntity target) {
        if (!(target instanceof Mob mob) || !BYeffect.canInfect(mob)
                || mob.hasEffect(ModEffects.QIANS.get())
                || NativeCorpseState.incubating(mob)
                || !(mob.level() instanceof ServerLevel level)) {
            WATER_EXPOSURE.remove(target);
            return;
        }

        long now = level.getGameTime();
        WaterExposure exposure = WATER_EXPOSURE.computeIfAbsent(mob, key -> new WaterExposure());

        // entityInside 同一 tick 可能被多个相邻尸水方块各调用一次，只累计一次
        if (exposure.lastTick == now) return;
        if (exposure.lastTick >= 0 && now - exposure.lastTick > EXPOSURE_GAP_TICKS) {
            exposure.ticks = 0;
        }
        exposure.lastTick = now;
        exposure.ticks++;

        // 鱼类接触尸水立即感染
        if (CorpseOriginServerConfig.fishImmediateInfection() && mob instanceof AbstractFish) {
            WATER_EXPOSURE.remove(mob);
            BYeffect.applyInfection(mob, level, CorpseOriginServerConfig.fishIncubationTicks(), null);
            return;
        }

        // 其它生物：连续浸泡满指定秒数后感染
        int requiredTicks = CorpseOriginServerConfig.waterSubmergeSeconds() * 20;
        if (exposure.ticks >= requiredTicks) {
            WATER_EXPOSURE.remove(mob);
            BYeffect.applyInfection(mob, level, CorpseOriginServerConfig.waterIncubationTicks(), null);
        }
    }

    /** 单只生物当前的连续浸泡进度。 */
    private static final class WaterExposure {
        private long lastTick = -1L;
        private int ticks;
    }
}
