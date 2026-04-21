package com.phagens.corpseorigin.entity.SegmentedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//头部
public abstract class AbstractSegmentedHead extends Monster {
    protected List<UUID> segmentUUIDs = new ArrayList<>();
    protected float totalMaxHealth = 100.0f;
    protected float currentTotalHealth;

    public AbstractSegmentedHead(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.currentTotalHealth = this.totalMaxHealth;
    }

    // 添加身体节段
    public void addSegment(AbstractSegmentedJoint segment) {
        this.segmentUUIDs.add(segment.getUUID());
        if (!level().isClientSide) {
            level().addFreshEntity(segment);
        }
    }

    //总血量管理
    public void damageTotal(float amount) {
        this.currentTotalHealth -= amount;
        if (this.currentTotalHealth <= 0) {
            this.discard();
        }
    }

    // --- 抽象方法：供子类实现具体的节段生成逻辑 ---
    public abstract AbstractSegmentedJoint createSegment(int index);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.currentTotalHealth = compound.getFloat("TotalHealth");

        this.segmentUUIDs.clear();
        ListTag listTag = compound.getList("Segments", 10); // 10 是 CompoundTag 的类型 ID
        for (int i = 0; i < listTag.size(); i++) {
            this.segmentUUIDs.add(NbtUtils.loadUUID(listTag.getCompound(i)));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("TotalHealth", this.currentTotalHealth);

        ListTag listTag = new ListTag();
        for (UUID uuid : this.segmentUUIDs) {
            listTag.add(NbtUtils.createUUID(uuid)); // 使用 createUUID 返回一个 CompoundTag
        }
        compound.put("Segments", listTag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }
}