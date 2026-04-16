package com.phagens.corpseorigin.entity.Animals;

import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ZbWormEntity extends PathfinderMob implements GeoEntity, ICorpseBrother, ICorpseHunger {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    
    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);

    public ZbWormEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherGatherGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherHiveMindGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackNonCorpsePlayer));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackCorpsePlayer));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 0, true, false, this::shouldAttackNonCorpseMob));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Animal.class, true));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, ZbWormEntity.class, 0, true, false, this::shouldAttackOtherCorpseEntity));
    }
    
    private boolean shouldAttackNonCorpsePlayer(LivingEntity entity) {
        return hungerSystem.shouldAttackNonCorpsePlayer(entity);
    }
    
    private boolean shouldAttackNonCorpseMob(LivingEntity entity) {
        return hungerSystem.shouldAttackNonCorpseMob(entity);
    }
    
    private boolean shouldAttackCorpsePlayer(LivingEntity entity) {
        return hungerSystem.shouldAttackCorpsePlayer(entity);
    }
    
    private boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        return hungerSystem.shouldAttackOtherCorpseEntity(entity);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends ZbWormEntity> PlayState predicate(AnimationState<T> animationState) {
        animationState.getController().setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        hungerSystem.tick();
    }

    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    public void setAttackCooldown() {
        attackCooldown = ATTACK_COOLDOWN_TICKS;
    }
    
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 记录被攻击的时间（用于反击逻辑）
        if (source.getEntity() instanceof LivingEntity) {
            hungerSystem.recordHurt();
        }
        return super.hurt(source, amount);
    }
    
    // ICorpseBrother 接口实现
    @Override
    public boolean isCorpseBrotherOf(net.minecraft.world.entity.Mob entity) {
        return entity instanceof ICorpseBrother;
    }
    
    @Override
    public void setHiveMindTarget(LivingEntity target) {
        hungerSystem.setHiveMindTarget(target);
    }
    
    @Override
    public LivingEntity getHiveMindTarget() {
        return hungerSystem.getHiveMindTarget();
    }
    
    @Override
    public boolean hasAttackTarget() {
        return this.getTarget() != null;
    }
    
    @Override
    public int getEvolutionLevel() {
        return hungerSystem.getEvolutionLevel();
    }
    
    // ICorpseHunger 接口实现
    @Override
    public int getCorpseHunger() {
        return hungerSystem.getCorpseHunger();
    }
    
    @Override
    public int getTicksExisted() {
        return this.tickCount;
    }
    
    @Override
    public Level getLevel() {
        return this.level();
    }
    
    @Override
    public net.minecraft.core.BlockPos blockPosition() {
        return super.blockPosition();
    }
    
    @Override
    public boolean isAlive() {
        return super.isAlive();
    }
    
    @Override
    public void setCorpseHunger(int hunger) {
        hungerSystem.setCorpseHunger(hunger);
    }
    
    @Override
    public void setEvolutionLevel(int level) {
        hungerSystem.setEvolutionLevel(level);
    }
}
