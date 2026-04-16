package com.phagens.corpseorigin.entity; 

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public interface ICorpseHunger {
    /**
     * 获取实体的当前游戏刻
     */
    int getTicksExisted();
    
    /**
     * 获取实体的世界
     */
    Level getLevel();
    
    /**
     * 获取实体的碰撞箱
     */
    AABB getBoundingBox();
    
    /**
     * 获取实体的位置
     */
    BlockPos blockPosition();
    
    /**
     * 检查实体是否活着
     */
    boolean isAlive();
    
    /**
     * 获取饥饿度
     */
    int getCorpseHunger();
    
    /**
     * 设置饥饿度
     */
    void setCorpseHunger(int hunger);
    
    /**
     * 获取进化等级
     */
    int getEvolutionLevel();
    
    /**
     * 设置进化等级
     */
    void setEvolutionLevel(int level);
    
    /**
     * 检查是否有攻击目标
     */
    boolean hasAttackTarget();
    
    /**
     * 检查是否是尸族成员
     */
    boolean isCorpseBrotherOf(net.minecraft.world.entity.Mob entity);
    
    /**
     * 设置集群意识目标
     */
    void setHiveMindTarget(LivingEntity target);
    
    /**
     * 获取集群意识目标
     */
    LivingEntity getHiveMindTarget();
}