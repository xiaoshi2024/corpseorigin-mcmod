package com.phagens.corpseorigin.entity.SegmentedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

//身体
public abstract class AbstractSegmentedJoint extends Entity {
    protected UUID previousUUID;
    protected float localMaxHealth = 20.0f;
    protected float currentLocalHealth;

    public AbstractSegmentedJoint(EntityType<?> type, Level level) {
        super(type, level);
        this.currentLocalHealth = this.localMaxHealth;
        this.noPhysics = true;
    }


    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        Entity prev = getPreviousEntity();
        if (prev != null && prev.isAlive()) {
            followEntity(prev);
        } else {
            this.discard();// 失去连接则死亡
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.hasUUID("PreviousUUID")) {
            this.previousUUID = compoundTag.getUUID("PreviousUUID");
        }
        this.currentLocalHealth = compoundTag.getFloat("LocalHealth");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        if (this.previousUUID != null) {
            compoundTag.putUUID("PreviousUUID", this.previousUUID);
        }
        compoundTag.putFloat("LocalHealth", this.currentLocalHealth);
    }

    protected void followEntity(Entity target) {
        //上节目标和当前位置
        Vec3 targetPos = target.position();
        Vec3 myPos = this.position();
        //计算两点距离
        double dist = myPos.distanceTo(targetPos);
        //根据子类定义间距
        double fixedDist = getSegmentDistance();
        //拉大追赶
        if (dist > fixedDist) {
            //1.计算向量 我指向目标 2.normalize 向量拉成一保留方向 3。每次移动0.4格
            Vec3 moveVec = targetPos.subtract(myPos).normalize().scale(0.4); // 0.4 是跟随系数
            //更新坐标
            this.setPos(myPos.x + moveVec.x, myPos.y + moveVec.y, myPos.z + moveVec.z);

            //自动砖头 看向移动方块
            this.setYRot((float) (Mth.atan2(moveVec.z, moveVec.x) * (180 / Math.PI)) - 90);
        }
    }

    public abstract double getSegmentDistance();

    public Entity getPreviousEntity() {
        if (previousUUID == null) return null;
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return serverLevel.getEntity(previousUUID);
        }
        return null;
    }
    public void setPreviousUUID(UUID uuid) {
        this.previousUUID = uuid;
    }
}
