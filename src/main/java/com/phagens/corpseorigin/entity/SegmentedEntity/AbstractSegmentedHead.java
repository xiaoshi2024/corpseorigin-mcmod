package com.phagens.corpseorigin.entity.SegmentedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//头部
public abstract class AbstractSegmentedHead extends Monster {
    // 同步数据到客户端
    protected static final EntityDataAccessor<Float> DATA_TOTAL_HEALTH =
            SynchedEntityData.defineId(AbstractSegmentedHead.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_MAX_HEALTH =
            SynchedEntityData.defineId(AbstractSegmentedHead.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_SEGMENT_COUNT =
            SynchedEntityData.defineId(AbstractSegmentedHead.class, EntityDataSerializers.INT);

    protected List<UUID> segmentUUIDs = new ArrayList<>();
    protected float baseHealthPerSegment = 30.0f; // 每节基础血量

    public AbstractSegmentedHead(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /**
     * 获取每节基础血量（由子类定义）
     */
    protected abstract float getBaseHealthPerSegment();

    /**
     * 计算当前应该有多少个节段
     */
    public int calculateExpectedSegmentCount() {
        float currentHealth = this.entityData.get(DATA_TOTAL_HEALTH);
        if (currentHealth <= 20) {
            return Math.max(1, (int) Math.ceil(currentHealth / getBaseHealthPerSegment()));
        }
        return Math.max(1, (int) (currentHealth / getBaseHealthPerSegment()));
    }

    /**
     * 添加身体节段
     */
    public void addSegment(AbstractSegmentedJoint segment) {
        this.segmentUUIDs.add(segment.getUUID());
        if (!level().isClientSide) {
            level().addFreshEntity(segment);
        }
    }

    /**
     * 移除指定节段
     */
    public void removeSegment(UUID segmentUUID) {
        this.segmentUUIDs.remove(segmentUUID);
    }

    /**
     * 从尾部删除多余节段（血量减少时调用）
     * @param targetCount 目标节段数量
     */
    public void trimExcessSegments(int targetCount) {
        if (level().isClientSide) return;

        while (segmentUUIDs.size() > targetCount && !segmentUUIDs.isEmpty()) {
            // 从尾部开始删除
            UUID lastUUID = segmentUUIDs.remove(segmentUUIDs.size() - 1);

            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(lastUUID);
                if (entity != null && entity.isAlive()) {
                    entity.discard(); // 删除实体
                }
            }
        }

        // 同步节段数量到客户端
        this.entityData.set(DATA_SEGMENT_COUNT, segmentUUIDs.size());
    }

    /**
     * 受到伤害时调整节段数量
     */
    public void onHealthChanged() {
        int expectedCount = calculateExpectedSegmentCount();
        trimExcessSegments(expectedCount);
    }

    /**
     * 设置总血量
     */
    public void setTotalHealth(float health) {
        float maxHealth = this.entityData.get(DATA_MAX_HEALTH);
        float clampedHealth = Math.max(0, Math.min(health, maxHealth));
        this.entityData.set(DATA_TOTAL_HEALTH, clampedHealth);

        // 血量变化后调整节段数量
        onHealthChanged();
    }

    /**
     * 获取当前总血量
     */
    public float getTotalHealth() {
        return this.entityData.get(DATA_TOTAL_HEALTH);
    }

    /**
     * 获取最大总血量
     */
    public float getMaxTotalHealth() {
        return this.entityData.get(DATA_MAX_HEALTH);
    }

    /**
     * 创建单个节段（由子类实现）
     */
    public abstract AbstractSegmentedJoint createSegment(int index);

    /**
     * 获取当前存活的节段列表（供子类实现）
     */
    public abstract List<? extends AbstractSegmentedJoint> getSegments();

    /**
     * 重建节段引用列表（读档后调用）
     * 从segmentUUIDs中查找世界中的实体，重建segments引用
     */
    public void rebuildSegmentReferences() {
        if (level().isClientSide) {
            org.apache.logging.log4j.LogManager.getLogger().warn("[AbstractSegmentedHead] rebuildSegmentReferences called on client side!");
            return;
        }

        org.apache.logging.log4j.LogManager.getLogger().info("[AbstractSegmentedHead] 开始重建，segmentUUIDs.size=" + segmentUUIDs.size());

        List<UUID> validUUIDs = new ArrayList<>();
        List<AbstractSegmentedJoint> validSegments = new ArrayList<>();

        if (level() instanceof ServerLevel serverLevel) {
            for (UUID uuid : segmentUUIDs) {
                Entity entity = serverLevel.getEntity(uuid);
                if (entity == null) {
                    org.apache.logging.log4j.LogManager.getLogger().warn("[AbstractSegmentedHead] UUID " + uuid + " 对应的实体未找到！");
                } else if (!(entity instanceof AbstractSegmentedJoint)) {
                    org.apache.logging.log4j.LogManager.getLogger().warn("[AbstractSegmentedHead] UUID " + uuid + " 对应的实体类型错误: " + entity.getClass().getSimpleName());
                } else if (!entity.isAlive()) {
                    org.apache.logging.log4j.LogManager.getLogger().warn("[AbstractSegmentedHead] UUID " + uuid + " 对应的实体已死亡");
                } else {
                    AbstractSegmentedJoint joint = (AbstractSegmentedJoint) entity;
                    validUUIDs.add(uuid);
                    validSegments.add(joint);

                    org.apache.logging.log4j.LogManager.getLogger().info("[AbstractSegmentedHead] 成功找到节段: " + uuid);

                    // 重新建立链式连接
                    if (validSegments.size() > 1) {
                        // 非第一节：前一节是上一个有效节段
                        AbstractSegmentedJoint prevSegment = validSegments.get(validSegments.size() - 2);
                        joint.setPreviousEntity(prevSegment);
                        org.apache.logging.log4j.LogManager.getLogger().info("[AbstractSegmentedHead] 设置节段 " + uuid + " 的前一节为 " + prevSegment.getUUID());
                    } else {
                        // 第一节：前一节是头部
                        joint.setPreviousEntity(this);
                        org.apache.logging.log4j.LogManager.getLogger().info("[AbstractSegmentedHead] 设置第一节的前一节为头部");
                    }
                }
            }
        }

        if (level() instanceof ServerLevel serverLevel) {
            List<AbstractSegmentedJoint> allJoints = serverLevel.getEntitiesOfClass(AbstractSegmentedJoint.class, this.getBoundingBox().inflate(50));
            for (AbstractSegmentedJoint joint : allJoints) {
                if (!validUUIDs.contains(joint.getUUID())) {
                    org.apache.logging.log4j.LogManager.getLogger().info("[AbstractSegmentedHead] 发现孤儿节段 " + joint.getUUID() + "，正在销毁...");
                    joint.discard();
                }
            }
        }


        // 更新UUID列表
        this.segmentUUIDs = validUUIDs;
        this.entityData.set(DATA_SEGMENT_COUNT, segmentUUIDs.size());

        org.apache.logging.log4j.LogManager.getLogger().info("[AbstractSegmentedHead] 重建完成，validSegments.size=" + validSegments.size());

        // 通知子类更新其segments引用列表（通过清空后重新添加）
        updateChildSegmentsList(validSegments);
    }

    /**
     * 更新子类的节段引用列表
     * 子类需要重写此方法来同步自己的segments列表
     */
    protected void updateChildSegmentsList(List<AbstractSegmentedJoint> validSegments) {
        // 默认空实现，由子类重写
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TOTAL_HEALTH, 150.0f);
        builder.define(DATA_MAX_HEALTH, 150.0f);
        builder.define(DATA_SEGMENT_COUNT, 5);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        float savedHealth = compound.getFloat("TotalHealth");
        this.entityData.set(DATA_TOTAL_HEALTH, savedHealth);
        this.entityData.set(DATA_MAX_HEALTH, compound.getFloat("MaxHealth"));

        this.segmentUUIDs.clear();
        ListTag listTag = compound.getList("Segments", 10);
        for (int i = 0; i < listTag.size(); i++) {
            this.segmentUUIDs.add(NbtUtils.loadUUID(listTag.getCompound(i)));
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("TotalHealth", this.entityData.get(DATA_TOTAL_HEALTH));
        compound.putFloat("MaxHealth", this.entityData.get(DATA_MAX_HEALTH));

        ListTag listTag = new ListTag();
        for (UUID uuid : this.segmentUUIDs) {
            listTag.add(NbtUtils.createUUID(uuid));
        }
        compound.put("Segments", listTag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        float currentHealth = this.entityData.get(DATA_TOTAL_HEALTH);
        float newHealth = currentHealth - amount;
        setTotalHealth(newHealth);
        this.setHealth(newHealth);
        // 死亡判定
        if (newHealth <= 0 && !this.isRemoved()) {
            this.die(this.damageSources().generic());
        }

        return true;
    }

    @Override
    public float getHealth() {
        return this.entityData.get(DATA_TOTAL_HEALTH);
    }

}