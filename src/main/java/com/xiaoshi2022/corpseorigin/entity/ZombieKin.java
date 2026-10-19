package com.xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 尸族工具接口（1.20.1 Forge 简化版）。
 * <p>
 * 移除了 PlayerCorpseComponent / LongYou 等上游依赖，
 * 只保留「半尸兄互伤规则」和「攻击判定」的核心逻辑。
 * 完整逻辑在 LowerLevelZbEntity 实现类中恢复。
 */
public interface ZombieKin {

    default boolean isTrueZombieKin() { return true; }
    default boolean isHybrid() { return false; }
    default int getHunger() { return 100; }
    default boolean isHungry() { return getHunger() < 30; }

    // ========== 静态工具方法 ==========

    static boolean isZombieKin(Entity entity) {
        if (entity == null) return false;
        if (com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.infected(entity)) return true;
        if (entity instanceof ZombieKin kin) return kin.isTrueZombieKin();
        return false;
    }

    static boolean isSameKin(Entity a, Entity b) {
        return isZombieKin(a) && isZombieKin(b);
    }

    static boolean canAttack(LivingEntity attacker, Entity target) {
        if (attacker == target) return false;
        if (!isZombieKin(target)) return true; // 目标不是尸族 → 可以攻击
        // 目标是尸族：非尸族攻击者允许互伤（默认）；尸族 NPC 按饥饿度判断
        if (attacker instanceof Mob mob && mob instanceof ZombieKin kin) {
            if (isRetaliating(attacker, target)) return true;
            return kin.isHungry();
        }
        return true;
    }

    static boolean isRetaliating(LivingEntity attacker, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) return false;
        LivingEntity lastAttacker = attacker.getLastHurtByMob();
        return lastAttacker == livingTarget;
    }
}
