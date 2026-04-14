package com.phagens.corpseorigin.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 鬼棍实体类（人类态）
 * 炎黄特能队第五小队成员，擅长棍术，会金针刺穴大法
 * 特征：紫色刺猬头，肤色煞白，眼睛很黑，身穿墨绿色外套，白色裤子
 */
public class GuigunEntity extends PathfinderMob implements GeoEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 动画定义
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");

    // 状态
    private boolean isUsingNeedleTechnique = false;
    private int needleTechniqueCooldown = 0;

    public GuigunEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 25.0D)  // 炎黄特能队成员，生命值较高
            .add(Attributes.MOVEMENT_SPEED, 0.35D)  // 速度较快
            .add(Attributes.ATTACK_DAMAGE, 6.0D)  // 攻击力较强
            .add(Attributes.ARMOR, 3.0D)  // 有一定护甲
            .add(Attributes.ATTACK_SPEED, 1.2D);  // 攻击速度较快
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return null; // 无环境音效（移除村民音效）
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT; // 保留受伤音效
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH; // 保留死亡音效
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private <E extends GuigunEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
        // 如果在攻击
        if (this.getAttackAnim(event.getPartialTick()) > 0) {
            return event.setAndContinue(ATTACK_ANIM);
        }

        // 快速移动
        if (this.getDeltaMovement().lengthSqr() > 0.1) {
            return event.setAndContinue(RUN_ANIM);
        }

        // 普通移动
        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }

        // 默认待机动画
        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 更新金针 technique 冷却
            if (needleTechniqueCooldown > 0) {
                needleTechniqueCooldown--;
                if (needleTechniqueCooldown <= 0) {
                    isUsingNeedleTechnique = false;
                }
            }

            // 随机使用金针 technique
            if (!isUsingNeedleTechnique && this.tickCount % 200 == 0 && this.random.nextFloat() < 0.1F) {
                useNeedleTechnique();
            }
        }
    }

    /**
     * 使用金针刺穴大法
     * 提升力量和速度，但会消耗生命值
     */
    private void useNeedleTechnique() {
        isUsingNeedleTechnique = true;
        needleTechniqueCooldown = 600; // 30秒

        // 提升属性
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.45D);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8.0D);

        // 消耗生命值
        this.hurt(this.damageSources().generic(), 5.0F);

        // 播放技能音效
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.playSound(null, this.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_STRONG,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide) {
            // 有概率使用金针刺穴大法
            if (!isUsingNeedleTechnique && this.getHealth() < this.getMaxHealth() * 0.5) {
                useNeedleTechnique();
            }
        }

        return result;
    }
}