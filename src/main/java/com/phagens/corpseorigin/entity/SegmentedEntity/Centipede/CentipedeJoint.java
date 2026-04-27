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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;


public class CentipedeJoint extends AbstractSegmentedJoint implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");

    private Vec3 lastPosition = Vec3.ZERO;
    private int contactDamageCooldown = 0;
    private static final int CONTACT_DAMAGE_COOLDOWN = 10; // 10 tick = 0.5秒
    public CentipedeJoint(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
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
        if (!level().isClientSide) {
            // 只有当头部彻底消失（不仅仅是死亡，而是被移除）时才考虑自毁
            if (this.previousEntity != null && this.previousEntity.isAlive()) {
                followEntity(previousEntity);
            } else {
                // 前一节已死亡或消失，说明链条断了，这个节段也应该消失
                this.discard();
                return;
            }


            if (contactDamageCooldown > 0) {
                contactDamageCooldown--;
            } else {
                List<Entity> nearbyEntities = level().getEntities(this,
                        this.getBoundingBox().inflate(0.5D),
                        entity -> entity instanceof LivingEntity
                                && entity != this.getPreviousEntity()
                                && !(entity instanceof CentipedeJoint));

                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity livingEntity && entity.isAlive()) {
                        livingEntity.hurt(this.damageSources().generic(), 2.0F);
                    }
                }
                contactDamageCooldown = CONTACT_DAMAGE_COOLDOWN;
            }
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

    //每个节间隔的距离
    @Override
    public double getSegmentDistance() {
        return 0.7;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private <E extends CentipedeJoint> PlayState controlAnimation(AnimationState<E> event) {
        Vec3 currentPosition = this.position();
        boolean isMoving = !currentPosition.equals(lastPosition);

        if (level().isClientSide) {
            if (isMoving) {
                lastPosition = currentPosition;
            }
        }

        if (isMoving) {
            return event.setAndContinue(RUN_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
