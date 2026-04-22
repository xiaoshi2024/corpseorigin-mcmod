package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede;

import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedJoint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * 蜈蚣身体节段（关节）实体
 *
 * 职责：
 * 1. 跟随前一节实体移动（链式跟随）
 * 2. 维护独立的局部血量
 * 3. 对周围生物造成接触伤害
 * 4. 失去连接时自动死亡（断裂机制）
 */
public class CentipedeJoint extends AbstractSegmentedJoint implements GeoEntity {
//同步血量
    private static final EntityDataAccessor<Float> DATA_LOCAL_HEALTH =
            SynchedEntityData.defineId(CentipedeJoint.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public CentipedeJoint(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.localMaxHealth = 30.0f;
        this.currentLocalHealth = this.localMaxHealth;
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)  // 每节30点血
                .add(Attributes.MOVEMENT_SPEED, 0.0D);  // 节段不主动移动
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOCAL_HEALTH, 30.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        this.entityData.set(DATA_LOCAL_HEALTH, this.currentLocalHealth);
        // 检测周围0.5格范围内的所有实体
        List<Entity> nearbyEntities = level().getEntities(this,
                this.getBoundingBox().inflate(0.5D),  // 扩大碰撞箱0.5格
                entity -> entity instanceof LivingEntity && entity != this.getPreviousEntity());
        // 对周围所有生物造成伤害（除了前一节）
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity livingEntity && entity.isAlive()) {
                livingEntity.hurt(this.damageSources().generic(), 2.0F);
            }
        }
    }


    @Override
    public double getSegmentDistance() {
        return 1.2;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {

        if (this.isInvulnerableTo(source)) {
            return false;
        }
        // 扣减局部血量
        this.currentLocalHealth -= amount;
        // 同步到客户端
        this.entityData.set(DATA_LOCAL_HEALTH, this.currentLocalHealth);
        // 通知头部更新总血量（仅服务端）
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getEntity(previousUUID) instanceof CentipedeHead head) {
                head.syncHealthToTotal();  // 实时同步总血量
            }
        }
        if (this.currentLocalHealth <= 0) {
            this.discard();
            return true;
        }
        return true;
    }


    /**
     * 从NBT读取存档数据
     */
    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        // 读取局部血量
        this.currentLocalHealth = compoundTag.getFloat("LocalHealth");

        // 读取局部最大血量
        this.localMaxHealth = compoundTag.getFloat("LocalMaxHealth");
    }

    /**
     * 写入NBT存档数据
     */
    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        // 保存当前局部血量
        compoundTag.putFloat("LocalHealth", this.currentLocalHealth);

        // 保存局部最大血量
        compoundTag.putFloat("LocalMaxHealth", this.localMaxHealth);
    }

    /**
     * 获取当前局部血量
     */
    public float getCurrentLocalHealth() {
        return this.currentLocalHealth;
    }

    /**
     * 设置当前局部血量
     * 通常由外部的血量同步逻辑调用
     */
    public void setCurrentLocalHealth(float health) {
        this.currentLocalHealth = health;
        this.entityData.set(DATA_LOCAL_HEALTH, health);
    }

    /**
     * 获取局部最大血量
     */
    public float getLocalMaxHealth() {
        return this.localMaxHealth;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


}
