package com.phagens.corpseorigin.entity.npc;

import com.phagens.corpseorigin.entity.ICorpseBrother;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 丑牛实体类
 * 尸兄组织成员，力量型角色
 */
public class ChouniuEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 动画定义（7种） ====================
    // 静止动画
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    // 移动动画
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("run");
    // 3个攻击动画
    protected static final RawAnimation ATTACK1_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation ATTACK2_ANIM = RawAnimation.begin().thenPlay("attack2");
    protected static final RawAnimation ATTACK3_ANIM = RawAnimation.begin().thenPlay("attackyu");
    // 防御动画（播放一次，播放期间为防御判定时间）
    protected static final RawAnimation DEFEND_ANIM = RawAnimation.begin().thenPlay("acctackzm");
    // 防御受击反击动画（防御中被打时触发）
    protected static final RawAnimation DEFEND_HIT_ANIM = RawAnimation.begin().thenPlay("acctackzm2");


    // ==================== 动画时长常量（tick） ====================
    private static final int ATTACK_ANIM_DURATION = 20;
    private static final int DEFEND_WINDOW_DURATION = 40;
    private static final int DEFEND_HIT_ANIM_DURATION = 160;
    private static final int DEFEND_COOLDOWN = 160;
    // ==================== 动画状态 ====================
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_STATE =
            SynchedEntityData.defineId(ChouniuEntity.class, EntityDataSerializers.INT);

    // 动画状态常量
    public static final int ANIM_NONE = 0;
    public static final int ANIM_ATTACK1 = 1;
    public static final int ANIM_ATTACK2 = 2;
    public static final int ANIM_ATTACK3 = 3;
    public static final int ANIM_DEFEND = 4;
    public static final int ANIM_DEFEND_HIT = 5;

    // 是否处于防御状态
    private boolean isDefending = false;

    private int animationTimer = 0;

    private int defendCooldown = 0;

    private int defendWindowTimer = 0;

    private Entity pendingAttackTarget = null;
    private int counterAttackTickCounter = 0;
    public ChouniuEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIMATION_STATE, ANIM_NONE);
    }

    public int getAnimationState() {
        return this.entityData.get(DATA_ANIMATION_STATE);
    }

    public void setAnimationState(int state) {
        this.entityData.set(DATA_ANIMATION_STATE, state);
    }

    @Override
    protected void registerGoals() {
        // TODO: 具体AI行为后续实现
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, false,
                entity -> entity instanceof ICorpseBrother));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.ARMOR, 25.0D);
    }
    //攻击触发

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (this.level().isClientSide) return super.doHurtTarget(entity);

        if (getAnimationState() == ANIM_DEFEND_HIT) return true;
        int attackType = this.random.nextInt(3);
        switch (attackType) {
            case 0 -> playAnimation(ANIM_ATTACK1, ATTACK_ANIM_DURATION);
            case 1 -> playAnimation(ANIM_ATTACK2, ATTACK_ANIM_DURATION);
            case 2 -> playAnimation(ANIM_ATTACK3, ATTACK_ANIM_DURATION);
        }

        this.swing(InteractionHand.MAIN_HAND);
        this.pendingAttackTarget = entity;
        return true;
    }

    private void executePendingAttack() {
        if (pendingAttackTarget == null || !pendingAttackTarget.isAlive()) {
            pendingAttackTarget = null;
            return;
        }

        boolean result = super.doHurtTarget(pendingAttackTarget);

        if (result) {
            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        pendingAttackTarget.getX(), pendingAttackTarget.getY() + pendingAttackTarget.getBbHeight() / 2, pendingAttackTarget.getZ(),
                        2, 0.2, 0.2, 0.2, 0);
            }
        }

        pendingAttackTarget = null;
    }

    //防御逻辑

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && isDefending && defendWindowTimer > 0) {
            setAnimationState(ANIM_DEFEND_HIT);
            this.animationTimer = DEFEND_HIT_ANIM_DURATION;
            isDefending = false;
            defendWindowTimer = 0;
            counterAttackTickCounter = 0;

            this.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.6F);

            return false;
        }

        return super.hurt(source, amount);
    }

    private void executeCounterAttackDamage() {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double range = 4.0;

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range))) {
            if (target == this || !target.isAlive()) continue;

            target.hurt(this.damageSources().mobAttack(this), damage);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        3, 0.3, 0.3, 0.3, 0.05);
            }
        }

        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F + this.random.nextFloat() * 0.3F);
    }
    // ==================== 防御触发时机 ====================

    private void tryTriggerDefend() {
        if (getAnimationState() != ANIM_NONE || isDefending || defendCooldown > 0) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = this.distanceTo(target);
        if (distance > 5.0) return;

        float healthPercent = this.getHealth() / this.getMaxHealth();
        float defendChance = healthPercent < 0.5F ? 0.08F : 0.04F;

        if (this.random.nextFloat() < defendChance) {
            isDefending = true;
            defendWindowTimer = DEFEND_WINDOW_DURATION;
            defendCooldown = DEFEND_COOLDOWN;
            this.getNavigation().stop();
        }
    }

    private void spawnDefendParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            double centerX = this.getX();
            double centerY = this.getY() + this.getBbHeight() / 2;
            double centerZ = this.getZ();
            double radius = 1.0;

            for (int i = 0; i < 30; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double offsetY = (this.random.nextDouble() - 0.5) * this.getBbHeight();

                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        centerX + offsetX, centerY + offsetY, centerZ + offsetZ,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }


    // ==================== GeckoLib 动画控制 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private PlayState controlAnimation(AnimationState<ChouniuEntity> event) {
        int state = getAnimationState();

        switch (state) {
            case ANIM_ATTACK1 -> { return event.setAndContinue(ATTACK1_ANIM); }
            case ANIM_ATTACK2 -> { return event.setAndContinue(ATTACK2_ANIM); }
            case ANIM_ATTACK3 -> { return event.setAndContinue(ATTACK3_ANIM); }
            case ANIM_DEFEND_HIT -> { return event.setAndContinue(DEFEND_HIT_ANIM); }
        }

        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }
        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== Tick 逻辑 ====================

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (isDefending && defendWindowTimer > 0) {
                defendWindowTimer--;
                spawnDefendParticles();
                if (defendWindowTimer <= 0) {
                    isDefending = false;
                }
            }

            if (animationTimer > 0) {
                animationTimer--;

                int currentState = getAnimationState();

                if (pendingAttackTarget != null
                        && (currentState == ANIM_ATTACK1 || currentState == ANIM_ATTACK2 || currentState == ANIM_ATTACK3)
                        && animationTimer == ATTACK_ANIM_DURATION / 2) {
                    executePendingAttack();
                }

                if (currentState == ANIM_DEFEND_HIT) {
                    counterAttackTickCounter++;
                    if (counterAttackTickCounter % 5 == 0) {
                        executeCounterAttackDamage();
                    }
                }

                if (animationTimer <= 0) {
                    if (currentState != ANIM_NONE) {
                        setAnimationState(ANIM_NONE);
                    }
                    pendingAttackTarget = null;
                    counterAttackTickCounter = 0;
                }
            }

            if (defendCooldown > 0) {
                defendCooldown--;
            }

            tryTriggerDefend();
        }
    }

    /**
     * 播放指定动画（带持续时间）
     */
    public void playAnimation(int state, int durationTicks) {
        if (this.level().isClientSide) return;
        setAnimationState(state);
        this.animationTimer = durationTicks;
    }
}
