// 文件路径: src/main/java/com/phagens/corpseorigin/entity/npc/MaotuProjectileEntity.java
package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class MaotuProjectileEntity extends Projectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(MaotuProjectileEntity.class, EntityDataSerializers.FLOAT);

    private int lifespan = 100;
    private int ticksExisted = 0;
    private float speed = 0.8F;
    private double turnRate = 0.15;
    private double searchRadius = 20.0;
    private int targetSearchCooldown = 0;
    private double explosionRadius = 3.0;

    private LivingEntity trackingTarget = null;
    private Consumer<LivingEntity> onHitCallback = null;

    public MaotuProjectileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public MaotuProjectileEntity(Level level, LivingEntity owner) {
        this(EntityRegistry.MAOTU_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 12.0F);
    }

    // ==================== 链式配置 ====================

    public MaotuProjectileEntity setDamage(float amount) {
        this.getEntityData().set(DAMAGE, amount);
        return this;
    }

    public MaotuProjectileEntity setLifespan(int ticks) {
        this.lifespan = ticks;
        return this;
    }

    public MaotuProjectileEntity setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public MaotuProjectileEntity setTurnRate(double turnRate) {
        this.turnRate = turnRate;
        return this;
    }

    public MaotuProjectileEntity setSearchRadius(double radius) {
        this.searchRadius = radius;
        return this;
    }

    public MaotuProjectileEntity setExplosionRadius(double radius) {
        this.explosionRadius = radius;
        return this;
    }

    public MaotuProjectileEntity setOnHitCallback(Consumer<LivingEntity> callback) {
        this.onHitCallback = callback;
        return this;
    }

    public void shootTowards(Vec3 direction) {
        Vec3 normalized = direction.normalize().scale(this.speed);
        this.setDeltaMovement(normalized);
        this.setYRot((float) (Math.atan2(normalized.x, normalized.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Math.atan2(normalized.y,
                Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)) * (180.0 / Math.PI)));
    }

    // ==================== Tick：弹道 + 追踪 ====================

    @Override
    public void tick() {
        super.tick();

        if (++ticksExisted > lifespan) {
            if (!this.level().isClientSide()) {
                explode();
            }
            this.discard();
            return;
        }

        if (!this.level().isClientSide()) {
            updateTracking();
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 pos = this.position();

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            this.discard();
            return;
        }

        this.setPos(pos.add(motion));

        if (this.horizontalCollision || this.verticalCollision) {
            if (!this.level().isClientSide()) {
                explode();
            }
            this.discard();
        }
    }

    // ==================== 追踪逻辑核心 ====================

    private void updateTracking() {
        if (trackingTarget != null) {
            if (!trackingTarget.isAlive()) {
                trackingTarget = null;
            } else {
                steerTowardsTarget();
            }
        }

        if (trackingTarget == null && targetSearchCooldown <= 0) {
            findNewTarget();
            targetSearchCooldown = 10;
        }

        if (targetSearchCooldown > 0) {
            targetSearchCooldown--;
        }
    }

    private void steerTowardsTarget() {
        Vec3 targetPos = trackingTarget.getPosition(1.0F)
                .add(0, trackingTarget.getBbHeight() * 0.5, 0);
        Vec3 toTarget = targetPos.subtract(this.position());
        Vec3 desiredDirection = toTarget.normalize();
        Vec3 currentDirection = this.getDeltaMovement().normalize();

        Vec3 newDirection = currentDirection.lerp(desiredDirection, turnRate).normalize();
        this.setDeltaMovement(newDirection.scale(this.speed));

        this.setYRot((float) (Math.atan2(newDirection.x, newDirection.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Math.atan2(newDirection.y,
                Math.sqrt(newDirection.x * newDirection.x + newDirection.z * newDirection.z)) * (180.0 / Math.PI)));
    }

    private void findNewTarget() {
        Entity owner = this.getOwner();
        if (owner == null) return;

        AABB searchBox = this.getBoundingBox().inflate(searchRadius);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                LivingEntity.class, searchBox,
                entity -> entity.isAlive()
                        && entity != owner
        );

        if (nearby.isEmpty()) return;

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity entity : nearby) {
            double dist = this.distanceToSqr(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }

        if (closest != null) {
            this.trackingTarget = closest;
        }
    }

    // ==================== 爆炸逻辑 ====================

    private void explode() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Entity owner = this.getOwner();
        Vec3 center = this.position();
        float damage = this.getEntityData().get(DAMAGE);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z, 15, 0.4, 0.4, 0.4, 0.1);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y + 0.3, center.z, 10, 0.3, 0.3, 0.3, 0.05);

        this.level().playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);

        AABB damageBox = new AABB(
                center.x - explosionRadius, center.y - explosionRadius, center.z - explosionRadius,
                center.x + explosionRadius, center.y + explosionRadius, center.z + explosionRadius
        );

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, damageBox,
                entity -> entity.isAlive()
                        && entity != owner
                        && !(owner != null && entity.isAlliedTo(owner))
                        && !(entity instanceof ICorpseBrother)
        );

        for (LivingEntity target : targets) {
            double dist = target.position().distanceTo(center);
            if (dist <= explosionRadius) {
                float falloff = (float) (1.0 - dist / explosionRadius);
                float actualDamage = damage * Math.max(0.4F, falloff);

                DamageSource damageSource = owner instanceof LivingEntity livingOwner
                        ? this.damageSources().indirectMagic(this, livingOwner)
                        : this.damageSources().magic();

                target.hurt(damageSource, actualDamage);

                Vec3 pushDir = target.position().subtract(center).normalize();
                target.push(pushDir.x * 0.6, 0.3, pushDir.z * 0.6);

                if (this.onHitCallback != null) {
                    this.onHitCallback.accept(target);
                }
            }
        }
    }

    // ==================== 命中回调 ====================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!this.level().isClientSide()) {
            explode();
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            explode();
            this.discard();
        }
    }

    // ==================== GeckoLib 动画 ====================

    protected static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("walk");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(FLY_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== NBT ====================

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Damage")) {
            this.setDamage(compound.getFloat("Damage"));
        }
        this.ticksExisted = compound.getInt("TicksExisted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Damage", this.getEntityData().get(DAMAGE));
        compound.putInt("TicksExisted", this.ticksExisted);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
