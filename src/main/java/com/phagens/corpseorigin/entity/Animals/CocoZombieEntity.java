package com.phagens.corpseorigin.entity.Animals;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.entity.ICorpseBrother;

public class CocoZombieEntity extends CocoPenguinEntity implements GeoEntity, ICorpseBrother {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final RawAnimation ZOMBIE_IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ZOMBIE_WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ZOMBIE_EAT_ANIM = RawAnimation.begin().thenPlay("eat");

    private int infectionTimer = 0;
    private static final int INFECTION_COOLDOWN = 60;

    private int zombieAttackCooldown = 0;
    private static final int ZOMBIE_ATTACK_COOLDOWN_TICKS = 30;

    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);

    public CocoZombieEntity(EntityType<? extends CocoPenguinEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(4, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherGatherGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherHiveMindGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // 【新增】攻击村民
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false,
                livingEntity -> !(livingEntity instanceof CocoZombieEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return CocoPenguinEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CocoZombieEntity>(this, "zombie_controller", 3, this::zombieAnimationPredicate));
    }

    private PlayState zombieAnimationPredicate(AnimationState<CocoZombieEntity> animationState) {
        // 攻击时直接播放进食动画
        if (zombieAttackCooldown > 0) {
            animationState.setAnimation(ZOMBIE_EAT_ANIM);
            return PlayState.CONTINUE;
        }

        // 进食冷却期间播放进食动画
        if (eatCooldown > 0 && !this.isInWater()) {
            animationState.setAnimation(ZOMBIE_EAT_ANIM);
            return PlayState.CONTINUE;
        }

        if (this.isInWater()) {
            animationState.setAnimation(ZOMBIE_WALK_ANIM);
            return PlayState.CONTINUE;
        }

        if (animationState.isMoving()) {
            animationState.setAnimation(ZOMBIE_WALK_ANIM);
            return PlayState.CONTINUE;
        }

        animationState.setAnimation(ZOMBIE_IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        super.tick();

        if (infectionTimer > 0) infectionTimer--;
        if (zombieAttackCooldown > 0) zombieAttackCooldown--;

        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceTo(target) < 3.0D && infectionTimer == 0) {
                attemptInfect(target);
            }
        }

        hungerSystem.tick();
    }

    private void attemptInfect(LivingEntity target) {
        if (target instanceof Player) {
            Player player = (Player) target;
            if (!player.isCreative() && !player.isSpectator()) {
                player.hurt(this.damageSources().mobAttack(this), 2.0F);
                this.playSound(SoundEvents.ZOMBIE_INFECT, 0.8F, 1.0F);
                infectionTimer = INFECTION_COOLDOWN;
            }
        } else if (target instanceof Animal) {
            Animal animal = (Animal) target;
            if (!animal.isBaby()) {
                animal.hurt(this.damageSources().mobAttack(this), 4.0F);
                this.playSound(SoundEvents.ZOMBIE_INFECT, 0.8F, 1.0F);
                infectionTimer = INFECTION_COOLDOWN;
            }
        }
    }

    protected void performAttack(LivingEntity target) {
        if (target == null || zombieAttackCooldown > 0) return;

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurt(this.damageSources().mobAttack(this), damage);
        this.playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 0.8F);
        zombieAttackCooldown = ZOMBIE_ATTACK_COOLDOWN_TICKS;

        if (!target.isAlive()) {
            addHunger(40);
            if (!this.isInWater()) {
                triggerEatAnimation();
            }
            this.setTarget(null);
        }
    }

    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("InfectionTimer", infectionTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        infectionTimer = compound.getInt("InfectionTimer");
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    // ICorpseBrother 接口实现
    @Override
    public boolean isCorpseBrotherOf(Mob entity) {
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
    public void setCorpseHunger(int hunger) {
        hungerSystem.setCorpseHunger(hunger);
    }

    @Override
    public void setEvolutionLevel(int level) {
        hungerSystem.setEvolutionLevel(level);
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

}
