package com.phagens.corpseorigin.entity.npc;

import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.MaotuProjectileEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 卯兔实体类 - 尸兄组织成员
 * 4动画：idle(循环)、walk(循环)、attack(近战)、attack2(远程追踪)
 */
public class MaotuEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 动画资源 ====================
    protected static final RawAnimation IDLE_ANIM    = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK_ANIM    = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ATTACK_ANIM  = RawAnimation.begin().thenPlay("attack2");
    protected static final RawAnimation ATTACK2_ANIM = RawAnimation.begin().thenPlay("attack");

    // ==================== 动画状态常量 ====================
    public static final int ANIM_NONE    = 0;
    public static final int ANIM_ATTACK  = 1;
    public static final int ANIM_ATTACK2 = 2;

    // ==================== 常量 ====================
    private static final int ANIM_DURATION = 20;
    private static final double MELEE_RANGE = 3.0D;
    private static final double RANGED_MIN_RANGE = 4.0D;
    private static final double RANGED_MAX_RANGE = 14.0D;
    private static final int PROJECTILE_COOLDOWN = 80;

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Integer> DATA_ANIM =
            SynchedEntityData.defineId(MaotuEntity.class, EntityDataSerializers.INT);

    // ==================== 状态字段 ====================
    private int animationTimer = 0;
    private int projectileCooldown = 0;
    private Entity pendingTarget = null;

    public MaotuEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIM, ANIM_NONE);
    }

    public int getAnimState() { return this.entityData.get(DATA_ANIM); }
    public void setAnimState(int s) { this.entityData.set(DATA_ANIM, s); }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> e instanceof ICorpseBrother));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ARMOR, 6.0D);
    }

    // ==================== 近战攻击入口 ====================

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (this.level().isClientSide) return super.doHurtTarget(entity);
        if (getAnimState() == ANIM_ATTACK2) return true;

        setAnimState(ANIM_ATTACK);
        this.animationTimer = ANIM_DURATION;
        this.swing(InteractionHand.MAIN_HAND);
        this.pendingTarget = entity;
        return true;
    }

    // ==================== 远程攻击 ====================

    private void tryFireProjectile() {
        if (getAnimState() != ANIM_NONE || projectileCooldown > 0) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double dist = this.distanceTo(target);
        if (dist < RANGED_MIN_RANGE || dist > RANGED_MAX_RANGE) return;

        setAnimState(ANIM_ATTACK2);
        this.animationTimer = ANIM_DURATION;
        this.projectileCooldown = PROJECTILE_COOLDOWN;
        this.getNavigation().stop();
    }

    private void fireHomingProjectile() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        float explosionDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F;
        Vec3 baseDirection = target.getEyePosition().subtract(this.position()).normalize();
        int missileCount = 10;
        for (int i = 0; i < missileCount; i++) {
            MaotuProjectileEntity projectile = new MaotuProjectileEntity(this.level(), this);

            double angleOffset = (i - (missileCount - 1) / 2.0) * 0.15;
            double pitchOffset = (this.random.nextDouble() - 0.5) * 0.3;

            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);
            Vec3 spreadDir = new Vec3(
                    baseDirection.x * cos - baseDirection.z * sin,
                    baseDirection.y + pitchOffset,
                    baseDirection.x * sin + baseDirection.z * cos
            ).normalize();

            projectile.shootTowards(spreadDir);
            projectile.setDamage(explosionDamage);
            projectile.setSpeed(0.7F + this.random.nextFloat() * 0.3F);
            projectile.setTurnRate(0.12 + this.random.nextDouble() * 0.06);
            projectile.setLifespan(100 + this.random.nextInt(40));
            projectile.setSearchRadius(20.0);
            projectile.setExplosionRadius(3.0);

            projectile.setOnHitCallback(hitTarget -> {
                this.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.6F, 1.4F);
            });

            this.level().addFreshEntity(projectile);
        }
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.7F);
    }

    // ==================== GeckoLib 动画控制器 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<MaotuEntity> event) {
        switch (getAnimState()) {
            case ANIM_ATTACK  -> { return event.setAndContinue(ATTACK_ANIM); }
            case ANIM_ATTACK2 -> { return event.setAndContinue(ATTACK2_ANIM); }
        }
        if (event.isMoving()) return event.setAndContinue(WALK_ANIM);
        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== Tick ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (getAnimState() == ANIM_ATTACK2) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        if (animationTimer > 0) {
            animationTimer--;
            int state = getAnimState();

            if (animationTimer == ANIM_DURATION - 10) {
                if (state == ANIM_ATTACK && pendingTarget != null && pendingTarget.isAlive()) {
                    super.doHurtTarget(pendingTarget);
                    this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                }
                if (state == ANIM_ATTACK2) {
                    fireHomingProjectile();
                }
            }

            if (animationTimer <= 0) {
                setAnimState(ANIM_NONE);
                pendingTarget = null;
            }
        }

        if (projectileCooldown > 0) projectileCooldown--;

        tryFireProjectile();
    }
}
