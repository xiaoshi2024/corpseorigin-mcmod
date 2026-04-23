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


public class CentipedeJoint extends AbstractSegmentedJoint implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int contactDamageCooldown = 0;
    private static final int CONTACT_DAMAGE_COOLDOWN = 10; // 10 tick = 0.5秒
    public CentipedeJoint(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 999.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (!hasNearbySegment()) {
            org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeJoint] 周围2格内无其他节段，自毁: " + this.getUUID());
            this.discard();
            return;
        }
        // 接触伤害冷却倒计时
        if (contactDamageCooldown > 0) {
            contactDamageCooldown--;
        } else {
            // 检测周围0.5格范围内的所有实体
            List<Entity> nearbyEntities = level().getEntities(this,
                    this.getBoundingBox().inflate(0.5D),
                    entity -> entity instanceof LivingEntity
                            && entity != this.getPreviousEntity()
                            && !(entity instanceof CentipedeJoint));  // ✅ 关键：排除其他蜈蚣节段

            // 对所有周围生物造成伤害（排除前一节和其他节段）
            for (Entity entity : nearbyEntities) {
                if (entity instanceof LivingEntity livingEntity && entity.isAlive()) {
                    livingEntity.hurt(this.damageSources().generic(), 2.0F);
                }
            }

            // 重置冷却时间
            contactDamageCooldown = CONTACT_DAMAGE_COOLDOWN;
        }
    }

    /**
     * 检查周围2格内是否有头部或其他节段
     */
    private boolean hasNearbySegment() {
        // 检查前一节是否存在且距离合理
        if (this.previousEntity != null && this.previousEntity.isAlive()) {
            double dist = this.distanceTo(this.previousEntity);
            if (dist <= 2.0) {
                return true;
            }
        }

        // 检查周围2格内是否有其他蜈蚣节段或头部
        List<Entity> nearby = level().getEntities(this,
                this.getBoundingBox().inflate(2.0),
                entity -> entity instanceof CentipedeHead || entity instanceof CentipedeJoint);

        return !nearby.isEmpty();
    }

    @Override
    public double getSegmentDistance() {
        return 1.5;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
