package com.phagens.corpseorigin.entity; 

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class CorpseHungerSystem {
    // 饥饿度系统 (0-100, 100为饱腹, 0为极度饥饿)
    private int hunger = 100;
    private static final int HUNGER_THRESHOLD_FOR_CANNIBALISM = 20; // 饥饿度低于20才允许吞噬
    
    // 尸体饱腹值系统 (0-3, 吞噬尸体获得)
    private int corpseHunger = 0;
    private static final int MAX_CORPSE_HUNGER = 3;
    
    // 被攻击记忆系统（用于反击）
    private int lastHurtTick = -1000; // 上次被攻击的游戏刻
    private static final int HURT_MEMORY_DURATION = 200; // 被攻击记忆持续时间（10秒）
    
    // 进化等级
    private int evolutionLevel = 1;
    
    // 集群意识目标
    private LivingEntity hiveMindTarget = null;
    
    // 主人UUID
    private java.util.UUID masterUUID = null;
    
    private final ICorpseHunger owner;
    
    public CorpseHungerSystem(ICorpseHunger owner) {
        this.owner = owner;
    }
    
    /**
     * 每100 tick（5秒）减少1点饥饿度
     */
    public void tick() {
        if (hunger > 0 && owner.getTicksExisted() % 100 == 0) {
            hunger--;
        }
    }
    
    /**
     * 记录被攻击的时间（用于反击逻辑）
     */
    public void recordHurt() {
        lastHurtTick = owner.getTicksExisted();
    }
    
    /**
     * 检查是否应该攻击非尸兄玩家
     */
    public boolean shouldAttackNonCorpsePlayer(LivingEntity entity) {
        if (entity instanceof ICorpseBrother) return false;
        if (!(entity instanceof Player player)) return false;
        if (masterUUID != null && player.getUUID().equals(masterUUID)) return false;
        return !com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player);
    }
    
    /**
     * 检查是否应该攻击非尸兄生物
     */
    public boolean shouldAttackNonCorpseMob(LivingEntity entity) {
        if (entity instanceof ICorpseBrother) return false;
        return true;
    }
    
    /**
     * 检查是否应该攻击尸兄玩家
     */
    public boolean shouldAttackCorpsePlayer(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        if (entity instanceof ICorpseBrother) return false;
        if (masterUUID != null && player.getUUID().equals(masterUUID)) return false;
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) return false;
        // 只有在被攻击时才反击尸兄玩家
        boolean wasRecentlyHurt = (owner.getTicksExisted() - lastHurtTick) < HURT_MEMORY_DURATION;
        return wasRecentlyHurt;
    }
    
    /**
     * 检查是否应该攻击其他尸兄实体
     */
    public boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        if (!(entity instanceof ICorpseBrother otherCorpse)) return false;
        // 只有在被攻击时才反击其他尸兄实体
        boolean wasRecentlyHurt = (owner.getTicksExisted() - lastHurtTick) < HURT_MEMORY_DURATION;
        return wasRecentlyHurt;
    }
    
    /**
     * 检查周围是否存在非同类目标（非尸兄的目标）
     * 非尸兄玩家、非尸兄怪物、动物都是优先攻击目标
     */
    public boolean hasNonZombieTargets() {
        if (!(owner.getLevel() instanceof ServerLevel level)) return false;

        var normalPlayers = level.getEntitiesOfClass(
                Player.class,
                owner.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackNonCorpsePlayer(entity)
        );
        if (!normalPlayers.isEmpty()) {
            return true;
        }

        var nonCorpseMobs = level.getEntitiesOfClass(
                Mob.class,
                owner.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackNonCorpseMob(entity)
        );
        if (!nonCorpseMobs.isEmpty()) {
            return true;
        }

        var animals = level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Animal.class,
                owner.getBoundingBox().inflate(16.0D),
                entity -> entity.isAlive()
        );
        if (!animals.isEmpty()) {
            return true;
        }

        return false;
    }
    
    /**
     * 检查是否在尸王领导下
     */
    public boolean isUnderZombieKingLeadership() {
        // 这里可以实现检查是否在龙右领导下的逻辑
        // 暂时返回false，后续可以根据实际情况修改
        return false;
    }
    
    /**
     * 检查是否饥饿
     */
    public boolean isHungry() {
        return this.hunger <= HUNGER_THRESHOLD_FOR_CANNIBALISM;
    }
    
    // Getters and Setters
    public int getHunger() {
        return hunger;
    }
    
    public void setHunger(int hunger) {
        this.hunger = Math.max(0, Math.min(100, hunger));
    }
    
    public int getCorpseHunger() {
        return corpseHunger;
    }
    
    public void setCorpseHunger(int corpseHunger) {
        this.corpseHunger = Math.max(0, Math.min(MAX_CORPSE_HUNGER, corpseHunger));
    }
    
    public int getEvolutionLevel() {
        return evolutionLevel;
    }
    
    public void setEvolutionLevel(int evolutionLevel) {
        this.evolutionLevel = Math.max(1, evolutionLevel);
    }
    
    public LivingEntity getHiveMindTarget() {
        return hiveMindTarget;
    }
    
    public void setHiveMindTarget(LivingEntity hiveMindTarget) {
        this.hiveMindTarget = hiveMindTarget;
    }
    
    public java.util.UUID getMasterUUID() {
        return masterUUID;
    }
    
    public void setMasterUUID(java.util.UUID masterUUID) {
        this.masterUUID = masterUUID;
    }
    
    // 保存数据
    public void saveData(net.minecraft.nbt.CompoundTag compound) {
        compound.putInt("Hunger", this.hunger);
        compound.putInt("CorpseHunger", this.corpseHunger);
        compound.putInt("EvolutionLevel", this.evolutionLevel);
        if (masterUUID != null) {
            compound.putUUID("MasterUUID", masterUUID);
        }
    }
    
    // 加载数据
    public void loadData(net.minecraft.nbt.CompoundTag compound) {
        if (compound.contains("Hunger")) {
            this.hunger = compound.getInt("Hunger");
        }
        if (compound.contains("CorpseHunger")) {
            this.corpseHunger = compound.getInt("CorpseHunger");
        }
        if (compound.contains("EvolutionLevel")) {
            this.evolutionLevel = compound.getInt("EvolutionLevel");
        }
        if (compound.contains("MasterUUID")) {
            this.masterUUID = compound.getUUID("MasterUUID");
        }
    }
}