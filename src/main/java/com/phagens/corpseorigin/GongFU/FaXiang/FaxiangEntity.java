package com.phagens.corpseorigin.GongFU.FaXiang;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * 法相实体 - 用于JS技能召唤的临时战斗实体
 * 【功能说明】
 * 1. 与玩家保持同一位置（瞬移跟随）
 * 2. 无碰撞箱，不影响玩家移动和视线
 * 3. 自动攻击玩家附近的敌人
 * 4. 支持GeckoLib动画系统
 * 5. 生命周期结束后自动消失
 */
public class FaxiangEntity extends PathfinderMob implements GeoEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 动画定义
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk"); //移动
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle"); //待机
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");//攻击

    // 生命周期管理
    public int lifespan = 600;
    private int age = 0;
    private double scale = 1.0D;
    
    // 召唤者信息
    private UUID ownerUUID = null;
    private LivingEntity cachedOwner = null;

    // 光环配置
    private double auraRadius = 8.0D;
    private int auraDamageInterval = 20;
    private int auraTimer = 0;
    
    // 资源路径
    private ResourceLocation modelResource = ResourceLocation.fromNamespaceAndPath("corpseorigin", "geo/entity/guigun.geo.json");
    private ResourceLocation textureResource = ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/guigun.png");
    private ResourceLocation animationResource = ResourceLocation.fromNamespaceAndPath("corpseorigin", "animations/entity/guigun.animation.json");

    public FaxiangEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvulnerable(false);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 50.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 10.0D)
            .add(Attributes.ARMOR, 5.0D)
            .add(Attributes.ATTACK_SPEED, 1.5D)
            .add(Attributes.FOLLOW_RANGE, 32.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FaxiangMeleeAttackGoal(this, 3, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, this::isValidTarget));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isValidTarget));
    }
    
    /**
     * 判断是否为有效攻击目标
     */
    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        LivingEntity owner = getOwner();
        if (owner != null) {
            if (target == owner) {
                return false;
            }
            
            if (target instanceof Player player && owner instanceof Player ownerPlayer) {
                if (player.getUUID().equals(ownerPlayer.getUUID())) {
                    return false;
                }
                
                if (player.getTeam() != null && ownerPlayer.getTeam() != null) {
                    if (player.getTeam().isAlliedTo(ownerPlayer.getTeam())) {
                        return false;
                    }
                }
            }
        }
        
        if (target instanceof FaxiangEntity) {
            return false;
        }
        
        return true;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private <E extends FaxiangEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
        if (this.getAttackAnim(event.getPartialTick()) > 0) {
            return event.setAndContinue(ATTACK_ANIM);
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

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            age++;

            if (age >= lifespan) {
                this.discard();
                return;
            }

            updateOwner();

            LivingEntity owner = getOwner();
            if (owner == null || !owner.isAlive()) {
                this.discard();
                return;
            }

            LivingEntity target = this.getTarget();
            if (target == null) {
                double distance = this.distanceToSqr(owner);
                if (distance > 16.0) {
                    this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                }
            }
        }
    }
    /**
     * 更新光环效果
     * 对光环范围内的敌人造成等于法相攻击力的伤害
     */
    private void updateAuraEffect() {
        auraTimer++;
        if (auraTimer < auraDamageInterval) {
            return;
        }
        auraTimer = 0;

        float attackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (attackDamage <= 0) {
            return;
        }

        AABB auraBox = this.getBoundingBox().inflate(auraRadius);
        List<LivingEntity> entitiesInRange = this.level().getEntitiesOfClass(
                LivingEntity.class,
                auraBox,
                entity -> isValidTarget(entity) && this.distanceToSqr(entity) <= auraRadius * auraRadius
        );

        for (LivingEntity entity : entitiesInRange) {
            DamageSource damageSource = this.damageSources().mobAttack(this);
            entity.hurt(damageSource, attackDamage);
        }
    }
    
    /**
     * 更新召唤者引用
     */
    private void updateOwner() {
        if (ownerUUID != null && (cachedOwner == null || !cachedOwner.isAlive())) {
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                cachedOwner = (LivingEntity) serverLevel.getEntity(ownerUUID);
            }
        }
    }
    
    /**
     * 每tick同步到召唤者位置
     */
    private void syncWithOwner() {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }
        
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }
    
    /**
     * 获取召唤者
     */
    public LivingEntity getOwner() {
        return cachedOwner;
    }
    
    /**
     * 设置召唤者
     */
    public void setOwner(LivingEntity owner) {
        this.cachedOwner = owner;
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    public void setLifespan(int ticks) {
        this.lifespan = ticks;
    }

    public void setScale(double scale) {
        this.scale = scale;
        this.refreshDimensions();
    }

    /**
     * 设置光环半径
     * @param radius 光环半径（格）
     */
    public void setAuraRadius(double radius) {
        this.auraRadius = radius;
    }

    /**
     * 设置光环伤害间隔
     * @param interval 伤害间隔（tick数，20=1秒）
     */
    public void setAuraDamageInterval(int interval) {
        this.auraDamageInterval = interval;
    }

    /**
     * 禁用碰撞箱
     */

    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
    
    public void setResources(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        this.modelResource = model;
        this.textureResource = texture;
        this.animationResource = animation;
    }
    
    public ResourceLocation getModelResource() {
        return this.modelResource;
    }
    
    public ResourceLocation getTextureResource() {
        return this.textureResource;
    }
    
    public ResourceLocation getAnimationResource() {
        return this.animationResource;
    }
}
