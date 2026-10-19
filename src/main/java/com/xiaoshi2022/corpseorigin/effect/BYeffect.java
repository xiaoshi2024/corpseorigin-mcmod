package com.xiaoshi2022.corpseorigin.effect;

import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import com.xiaoshi2022.corpseorigin.entity.HybridZombie;
import com.xiaoshi2022.corpseorigin.registry.ModEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「千丝」感染效果（BYeffect）—— 1.20.1 Forge 版。
 * <p>
 * Forge 1.20.1 MobEffect API：
 * - applyEffectTick(LivingEntity, int) → void（无返回值，override 父类）
 * - isDurationEffectTick(int, int) → boolean（判断这 tick 是否触发 applyEffectTick）
 */
public class BYeffect extends MobEffect {

    private static final Map<UUID, UUID> infectionSource = new HashMap<>();
    private static final Map<UUID, Integer> TOTAL_DURATION = new HashMap<>();

    public BYeffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    // Forge 1.20.1: void applyEffectTick
    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        MobEffectInstance instance = livingEntity.getEffect(ModEffects.QIANS.get());
        if (instance == null) return;
        int remaining = instance.getDuration();
        // 无限时长 = 已转化后的同步标记，不做任何处理
        if (remaining < 0) return;

        if (remaining <= 1) {
            if (!livingEntity.level().isClientSide() && livingEntity.isAlive()) {
                if (livingEntity instanceof Mob mob && canInfect(mob)) {
                    NativeCorpseState.transform(mob, (net.minecraft.server.level.ServerLevel) livingEntity.level());
                }
                TOTAL_DURATION.remove(livingEntity.getUUID());
                infectionSource.remove(livingEntity.getUUID());
            }
        }
    }

    // Forge 1.20.1: boolean isDurationEffectTick(int duration, int amplifier)
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0 || duration <= 1;
    }

    // ==================== 静态方法 ====================

    public static void applyInfection(LivingEntity target, net.minecraft.server.level.ServerLevel serverLevel) {
        applyInfection(target, serverLevel, null);
    }

    public static void applyInfection(LivingEntity target, net.minecraft.server.level.ServerLevel serverLevel, UUID sourceUUID) {
        if (serverLevel == null) return;
        applyInfection(target, serverLevel, 60 + serverLevel.getRandom().nextInt(241), sourceUUID);
    }

    public static void applyInfection(LivingEntity target, net.minecraft.server.level.ServerLevel serverLevel,
                                      int durationTicks, UUID sourceUUID) {
        if (serverLevel == null || !canInfect(target) || target.hasEffect(ModEffects.QIANS.get())
                || NativeCorpseState.incubating(target)) return;
        int duration = Math.max(1, durationTicks);
        if (sourceUUID != null) infectionSource.put(target.getUUID(), sourceUUID);
        TOTAL_DURATION.put(target.getUUID(), duration);
        if (target instanceof Mob) {
            String prefix = HybridZombie.INFECTION_GENERATION + "_pending:";
            for (String tag : java.util.List.copyOf(target.getTags()))
                if (tag.startsWith(prefix)) target.removeTag(tag);
            Entity source = sourceUUID == null ? null : serverLevel.getEntity(sourceUUID);
            NativeCorpseState.captureSource(target, source);
            int generation = source == null ? 1 : Math.max(1, HybridZombie.generation(source) + 1);
            target.addTag(prefix + generation);
        }
        boolean applied = target.addEffect(new MobEffectInstance(ModEffects.QIANS.get(), duration, 0, false, true, true));
        if (!applied && target instanceof Mob) {
            NativeCorpseState.setIncubation(target, duration);
        } else if (!applied) {
            infectionSource.remove(target.getUUID());
            TOTAL_DURATION.remove(target.getUUID());
        }
    }

    public static boolean canInfect(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (NativeCorpseState.infected(target)) return false;
        if (target instanceof com.xiaoshi2022.corpseorigin.entity.ZombieKin) return false;
        // 参考 26.2：所有生物（含模组生物）都可以被尸兄化，不再限制 minecraft 命名空间
        return target instanceof Mob;
    }

    public static UUID getInfectionSource(UUID targetUUID) { return infectionSource.get(targetUUID); }
    public static void clearInfectionSource(UUID targetUUID) { infectionSource.remove(targetUUID); }
    public static void clearTotalDuration(UUID targetUUID) { TOTAL_DURATION.remove(targetUUID); }
}
