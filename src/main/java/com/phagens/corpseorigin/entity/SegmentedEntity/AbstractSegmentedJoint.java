package com.phagens.corpseorigin.entity.SegmentedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

//身体
public abstract class AbstractSegmentedJoint extends Monster {
    protected Entity previousEntity;
    public AbstractSegmentedJoint(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端不执行游戏逻辑
        if (level().isClientSide) return;

        // 跟随前一节移动（即使前一节死亡也要继续跟随，避免闪烁）
        if (previousEntity != null) {
            followEntity(previousEntity);
        }
    }

    /**
     * 链式跟随算法
     */
    protected void followEntity(Entity target) {
        Vec3 targetPos = target.position();
        Vec3 myPos = this.position();

        // 获取目标的朝向角度（转换为弧度）
        float targetYaw = target.getYRot();
        double yawRad = Math.toRadians(targetYaw);

        // 计算目标后方的期望位置（翻转符号）
        double expectedX = targetPos.x + Math.sin(yawRad) * getSegmentDistance();
        double expectedZ = targetPos.z - Math.cos(yawRad) * getSegmentDistance();

        // Y轴保持与目标相同的高度
        double expectedY = targetPos.y;

        // 计算当前位置到期望位置的偏移
        double dx = expectedX - myPos.x;
        double dy = expectedY - myPos.y;
        double dz = expectedZ - myPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 如果距离超过阈值，向期望位置移动
        if (dist > 0.1) {
            double speed = 0.5;
            double moveX = (dx / dist) * speed;
            double moveY = (dy / dist) * speed;
            double moveZ = (dz / dist) * speed;

            this.setPos(myPos.x + moveX, myPos.y + moveY, myPos.z + moveZ);
            this.setYRot(targetYaw);
        }
    }

    /**
     * 获取相邻节段的固定间距（由子类定义）
     */
    public abstract double getSegmentDistance();

    /**
     * 设置前一节实体引用
     */
    public void setPreviousEntity(Entity entity) {
        this.previousEntity = entity;
    }

    /**
     * 获取前一节实体
     */
    public Entity getPreviousEntity() {
        return this.previousEntity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

}
