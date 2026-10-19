package com.xiaoshi2022.corpseorigin.growth;

import com.xiaoshi2022.corpseorigin.entity.HybridZombie;
import com.xiaoshi2022.corpseorigin.entity.ZombieKin;
import com.xiaoshi2022.corpseorigin.registry.ModEffects;
import com.xiaoshi2022.corpseorigin.effect.BYeffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 感染触发链路入口（1.20.1 Forge 版本）。
 * <p>
 * 与 26.2 Fabric 版本的差异：ServerLivingEntityEvents → Forge LivingHurtEvent。
 * 事件注册在 {@link com.xiaoshi2022.corpseorigin.event.ModEvents}。
 */
public final class CorpseInfection {
    private static final Map<LivingEntity, Long> nextWaterRoll = new WeakHashMap<>();
    private CorpseInfection() {}

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

    /** 尸水接触入口：由 InfectedWaterBlock.entityInside 调用。 */
    public static void touchInfectedWater(LivingEntity target) {
        if (!(target instanceof Mob) || !BYeffect.canInfect(target)
                || target.hasEffect(ModEffects.QIANS.get())
                || NativeCorpseState.incubating(target)
                || !(target.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now < nextWaterRoll.getOrDefault(target, Long.MIN_VALUE)) return;
        nextWaterRoll.put(target, now + 20); // 每 1 秒最多一次概率判定
        // 默认 30% 几率，未来可接入配置；感染时长 200 tick（10 秒）
        if (target.getRandom().nextDouble() < 0.3)
            BYeffect.applyInfection(target, level, 200, null);
    }
}
