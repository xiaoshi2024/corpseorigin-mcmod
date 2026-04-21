package com.phagens.corpseorigin.entity.runEntityBeam.Entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class LaserBeamEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Float> DATA_LENGTH = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity owner;
    private UUID ownerUUID;
    private float damage = 5.0f;
    private int damageCooldown = 0;
    private int damageInterval = 10;

    public LaserBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, 1.0f);
        builder.define(DATA_LIFE, 60);
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            return;
        }

        syncOwnerFromUUID();

        int currentLife = entityData.get(DATA_LIFE);
        if (--currentLife <= 0 || owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        entityData.set(DATA_LIFE, currentLife);

        setPos(getMouthPosition());
        performRaycastAndDamage();

        if (damageCooldown > 0) damageCooldown--;
    }

    private void syncOwnerFromUUID() {
        if (owner == null && ownerUUID != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(ownerUUID);
            if (entity instanceof LivingEntity livingEntity) {
                owner = livingEntity;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.damage = compoundTag.getFloat("Damage");
        this.damageCooldown = compoundTag.getInt("DamageCooldown");
        this.damageInterval = compoundTag.getInt("DamageInterval");
        if (compoundTag.hasUUID("OwnerUUID")) {
            this.ownerUUID = compoundTag.getUUID("OwnerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putFloat("Damage", this.damage);
        compoundTag.putInt("DamageCooldown", this.damageCooldown);
        compoundTag.putInt("DamageInterval", this.damageInterval);
        if (this.ownerUUID != null) {
            compoundTag.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    private void performRaycastAndDamage() {
        if (owner == null) return;

        Vec3 startPos = getMouthPosition();
        Vec3 lookVec = owner.getLookAngle().normalize();

        double maxRange = 256.0;
        Vec3 endPos = startPos.add(lookVec.scale(maxRange));

        BlockHitResult blockHit = level().clip(new ClipContext(
                startPos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        float newLength;
        if (blockHit.getType() == HitResult.Type.MISS) {
            newLength = (float) maxRange;
        } else {
            newLength = (float) startPos.distanceTo(blockHit.getLocation());
        }

        entityData.set(DATA_LENGTH, newLength);

        if (damageCooldown <= 0) {
            applyDamageAlongBeam(startPos, lookVec, newLength);
            damageCooldown = damageInterval;
        }
    }

    private void applyDamageAlongBeam(Vec3 startPos, Vec3 direction, float length) {
        Vec3 endPos = startPos.add(direction.scale(length));
        AABB beamBox = new AABB(startPos, endPos).inflate(0.3);

        List<Entity> entities = level().getEntitiesOfClass(Entity.class, beamBox,
                e -> e != owner && e != this && e.isAlive() && e instanceof LivingEntity);

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingTarget) {
                Vec3 targetPos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                Vec3 toTarget = targetPos.subtract(startPos);
                double projection = toTarget.dot(direction);

                if (projection >= -0.5 && projection <= length + 0.5) {
                    Vec3 closestPoint = startPos.add(direction.scale(Math.max(0, projection)));
                    double distance = closestPoint.distanceTo(targetPos);

                    if (distance <= entity.getBbWidth() / 2.0 + 0.3) {
                        livingTarget.hurt(level().damageSources().mobAttack(owner), damage);
                        livingTarget.knockback(0.3, direction.x, direction.z);
                    }
                }
            }
        }
    }

    private Vec3 getMouthPosition() {
        if (owner == null) return position();
        return owner.getEyePosition(1f).add(owner.getLookAngle().scale(0.5));
    }



    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public float getCurrentLength() {
        return entityData.get(DATA_LENGTH);
    }

    public void setCurrentLength(float currentLength) {
        entityData.set(DATA_LENGTH, currentLength);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }
}
