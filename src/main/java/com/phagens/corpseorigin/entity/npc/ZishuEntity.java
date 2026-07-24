package com.phagens.corpseorigin.entity.npc;

import com.phagens.corpseorigin.entity.ICorpseBrother;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 子鼠实体类 - 敏捷型
 * 4动画：idle(walk循环)、walk(run循环)、attack(近战)、attack2(远程)
 */
public class ZishuEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 动画 ====================
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack2");
    protected static final RawAnimation SUMMON_ANIM = RawAnimation.begin().thenPlay("attack");

    // ==================== 常量 ====================
    private static final int ANIM_DURATION = 20;
    private static final double MELEE_RANGE = 4.0D;
    private static final double SUMMON_RANGE = 8.0D;
    private static final int SUMMON_COOLDOWN = 100;

    // ==================== 状态 ====================
    private static final EntityDataAccessor<Integer> DATA_ANIM =
            SynchedEntityData.defineId(ZishuEntity.class, EntityDataSerializers.INT);

    public static final int ANIM_NONE = 0;
    public static final int ANIM_ATTACK = 1;
    public static final int ANIM_SUMMON = 2;

    private int animationTimer = 0;
    private int summonCooldown = 0;
    private Entity pendingTarget = null;

    public ZishuEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIM, ANIM_NONE);
    }

    public int getAnimState() { return this.entityData.get(DATA_ANIM); }
    public void setAnimState(int s) { this.entityData.set(DATA_ANIM, s); }

    // ==================== AI ====================

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
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ARMOR, 8.0D);
    }

    // ==================== 近战 ====================

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (this.level().isClientSide) return super.doHurtTarget(entity);
        if (getAnimState() == ANIM_SUMMON) return true;

        setAnimState(ANIM_ATTACK);
        this.animationTimer = ANIM_DURATION;
        this.swing(InteractionHand.MAIN_HAND);
        this.pendingTarget = entity;
        return true;
    }

    // ==================== 远程 ====================

    private void trySummon() {
        if (getAnimState() != ANIM_NONE || summonCooldown > 0) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double dist = this.distanceTo(target);
        if (dist <= MELEE_RANGE || dist > SUMMON_RANGE) return;

        setAnimState(ANIM_SUMMON);
        this.animationTimer = ANIM_DURATION;
        this.summonCooldown = SUMMON_COOLDOWN;
        this.getNavigation().stop();
    }

    private void doSummonDamage() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        if (this.level() instanceof ServerLevel serverLevel) {
            // 在目标位置召唤闪电
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (lightning != null) {
                lightning.moveTo(target.getX(), target.getY(), target.getZ());
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);
            }
            // 爆炸粒子特效
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + 0.5, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        }

        this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 0.8F);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);

        // 伤害 = 攻击力 * 6
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 4.0F;
        target.hurt(this.damageSources().mobAttack(this), damage);
    }

    // ==================== 动画控制 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<ZishuEntity> event) {
        switch (getAnimState()) {
            case ANIM_ATTACK -> { return event.setAndContinue(ATTACK_ANIM); }
            case ANIM_SUMMON -> { return event.setAndContinue(SUMMON_ANIM); }
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

        // 召唤动画期间锁定移动
        if (getAnimState() == ANIM_SUMMON) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        if (animationTimer > 0) {
            animationTimer--;
            int state = getAnimState();

            // 第15tick触发近战伤害
            if (animationTimer == ANIM_DURATION - 15) {
                if (state == ANIM_ATTACK && pendingTarget != null && pendingTarget.isAlive()) {
                    super.doHurtTarget(pendingTarget);
                    this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                }
            }

            // 动画结束时触发召唤伤害
            if (animationTimer <= 0) {
                if (state == ANIM_SUMMON) {
                    doSummonDamage();
                }
                setAnimState(ANIM_NONE);
                pendingTarget = null;
            }
        }

        if (summonCooldown > 0) summonCooldown--;

        trySummon();
    }
}
