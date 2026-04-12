package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.entity.EntityAI.JLAI.ModFollow;
import com.phagens.corpseorigin.entity.EntityAI.Vibrationsys.ModVibrationUser;
import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ZbrFishEntity extends AbstractFish implements GeoEntity, VibrationSystem, ICorpseBrother {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    
    private final DynamicGameEventListener<Listener> dynamicGameEventListener;
    private final VibrationSystem.User vibrationUser;
    private VibrationSystem.Data vibrationData;
    
    private int evolutionLevel = 1;
    private int hunger = 100;
    private static final int HUNGER_THRESHOLD = 20;
    private int lastHurtTick = -1000;
    private static final int HURT_MEMORY_DURATION = 200;
    
    private int corpseHunger = 0;
    private static final int MAX_CORPSE_HUNGER = 3;
    private int acidSprayCooldown = 0;
    private static final int ACID_SPRAY_COOLDOWN_TICKS = 60;

    private LivingEntity hiveMindTarget = null;

    public ZbrFishEntity(EntityType<? extends AbstractFish> entityType, Level level) {
        super(entityType, level);
        this.vibrationUser = new ModVibrationUser(this);
        this.vibrationData = new VibrationSystem.Data();
        this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    }
    
    // 自定义鱼类移动控制
    private static class FishMoveControl extends MoveControl {
        private final ZbrFishEntity fish;
        
        public FishMoveControl(ZbrFishEntity fish) {
            super(fish);
            this.fish = fish;
        }
        
        @Override
        public void tick() {
            if (this.fish.isInWater()) {
                this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
            }
            
            if (this.operation == Operation.MOVE_TO && !this.fish.getNavigation().isDone()) {
                double dx = this.wantedX - this.fish.getX();
                double dy = this.wantedY - this.fish.getY();
                double dz = this.wantedZ - this.fish.getZ();
                
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dy = dy / distance;
                float speed = (float)(this.speedModifier * this.fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
                
                this.fish.setSpeed(speed);
                this.fish.setXRot(this.rotlerp(this.fish.getXRot(), (float)(-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180D / Math.PI))), 10.0F));
                this.fish.yHeadRot = this.fish.getXRot();
                
                double movementFactor = this.fish.isInWater() ? 0.1D : 0.02D;
                this.fish.setDeltaMovement(this.fish.getDeltaMovement().add(dx * movementFactor, dy * movementFactor, dz * movementFactor));
            } else {
                this.fish.setSpeed(0.0F);
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(1, new com.phagens.corpseorigin.entity.EntityAI.JLAI.AquaticSeekCorpseGibGoal(
            this, 
            this::eatCorpseGib,
            () -> this.corpseHunger
        ));
        this.goalSelector.addGoal(2, new ModFollow(this, 1.0D, true));
        this.goalSelector.addGoal(3, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherGatherGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }
    
    protected void addBehaviourGoals() {
        this.targetSelector.addGoal(1, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherHiveMindGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackNonCorpsePlayer));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 0, true, false, this::shouldAttackNonCorpseMob));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Animal.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackCorpsePlayer));
    }
    
    private boolean shouldAttackNonCorpsePlayer(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof ICorpseBrother) return false;
        if (!(entity instanceof Player player)) return false;
        return !com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player);
    }
    
    private boolean shouldAttackNonCorpseMob(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof ICorpseBrother) return false;
        if (entity instanceof Player) return false;
        return true;
    }
    
    private boolean shouldAttackCorpsePlayer(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        if (entity instanceof ICorpseBrother) return false;
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) return false;
        boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
        if (wasRecentlyHurt) return true;
        return this.hunger <= HUNGER_THRESHOLD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "swim_controller", 5, this::swimAnimation));
    }

    private <E extends ZbrFishEntity> software.bernie.geckolib.animation.PlayState swimAnimation(AnimationState<E> event) {
        if (event.isMoving()) {
            return event.setAndContinue(SWIM_ANIM);
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
            VibrationSystem.Ticker.tick((net.minecraft.server.level.ServerLevel) this.level(), this.vibrationData, this.vibrationUser);
            
            if (this.tickCount % 100 == 0 && hunger > 0) {
                hunger--;
            }
            
            if (this.tickCount % 200 == 0) {
                if (this.getHealth() < this.getMaxHealth()) {
                    float healAmount = 0.5f + (evolutionLevel * 0.3f);
                    this.heal(healAmount);
                }
            }
            
            if (acidSprayCooldown > 0) {
                acidSprayCooldown--;
            }
            
            if (this.tickCount % 40 == 0) {
                tryEatNearbyCorpseGib();
            }
            
            if (this.corpseHunger >= 1 && this.tickCount % 60 == 0) {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    double dist = this.distanceTo(target);
                    if (dist > 2.0D && dist <= 8.0D) {
                        sprayAcidAtTarget(target);
                    }
                }
            }
        }
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 记录被攻击的时间（用于反击逻辑）
        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            lastHurtTick = this.tickCount;
        }
        return super.hurt(source, amount);
    }
    
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // 尸族免疫普通毒素
        if (effect.getEffect().value() == net.minecraft.world.effect.MobEffects.POISON.value() ||
            effect.getEffect().value() == net.minecraft.world.effect.MobEffects.HUNGER.value() ||
            effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            return false;
        }
        return super.canBeAffected(effect);
    }
    
    @Override
    public Data getVibrationData() {
        return this.vibrationData;
    }
    
    @Override
    public User getVibrationUser() {
        return this.vibrationUser;
    }
    
    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence();
    }
    
    @Override
    public int getMaxAirSupply() {
        return 300;
    }
    
    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.isInWater() && this.onGround() && this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4F, (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F));
            this.hasImpulse = true;
            this.playSound(this.getFlopSound(), this.getSoundVolume(), 1.0F);
        }
    }
    
    protected net.minecraft.sounds.SoundEvent getFlopSound() {
        return net.minecraft.sounds.SoundEvents.COD_FLOP;
    }
    
    @Override
    public boolean isPushedByFluid() {
        return false;
    }
    
    @Override
    public boolean shouldDespawnInPeaceful() {
        return true;
    }
    
    // 使尸兄鱼能够在水中呼吸
    @Override
    public int getAirSupply() {
        return 300;
    }
    
    @Override
    public net.minecraft.world.item.ItemStack getBucketItemStack() {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("EvolutionLevel", this.evolutionLevel);
        compound.putInt("Hunger", this.hunger);
        compound.putInt("LastHurtTick", this.lastHurtTick);
        compound.putInt("CorpseHunger", this.corpseHunger);
        compound.putInt("AcidSprayCooldown", this.acidSprayCooldown);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("EvolutionLevel")) {
            this.evolutionLevel = compound.getInt("EvolutionLevel");
        }
        if (compound.contains("Hunger")) {
            this.hunger = compound.getInt("Hunger");
        }
        if (compound.contains("LastHurtTick")) {
            this.lastHurtTick = compound.getInt("LastHurtTick");
        }
        if (compound.contains("CorpseHunger")) {
            this.corpseHunger = compound.getInt("CorpseHunger");
        }
        if (compound.contains("AcidSprayCooldown")) {
            this.acidSprayCooldown = compound.getInt("AcidSprayCooldown");
        }
    }
    
    /**
     * 攻击目标时，有一定概率感染村民
     * 尸族特性：攻击活物时恢复生命值和饥饿度
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        // 检查是否应该攻击尸兄玩家
        if (entity instanceof Player player) {
            if (com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
                // 如果被攻击了，允许反击
                boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
                if (!wasRecentlyHurt) {
                    // 同类尸兄玩家，只有在极度饥饿时才攻击
                    boolean isHungry = this.hunger <= HUNGER_THRESHOLD;
                    if (!isHungry) {
                        return false; // 不饥饿时不攻击同类
                    }
                }
            }
        }
        
        boolean result = super.doHurtTarget(entity);
        
        if (result && !this.level().isClientSide) {
            // 尸族特性：攻击活物时恢复生命值和饥饿度
            if (entity instanceof net.minecraft.world.entity.LivingEntity target) {
                // 恢复饥饿度
                hunger = Math.min(100, hunger + 5);
                
                // 恢复生命值
                float healAmount = 1.0f + (evolutionLevel * 0.5f);
                this.heal(healAmount);
            }
            
            // 随机感染村民
            if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
                if (this.random.nextFloat() < 0.3) { // 30% 概率感染
                    infectVillager(villager);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 感染村民为尸兄
     */
    private void infectVillager(net.minecraft.world.entity.npc.Villager villager) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        
        com.phagens.corpseorigin.effect.BYeffect.applyInfection(villager, serverLevel);
    }
    
    /**
     * 尸体饱腹值相关方法
     */
    @Override
    public int getCorpseHunger() {
        return this.corpseHunger;
    }

    @Override
    public boolean isCorpseBrotherOf(Mob entity) {
        return entity instanceof ICorpseBrother;
    }

    @Override
    public void setHiveMindTarget(LivingEntity target) {
        this.hiveMindTarget = target;
    }

    @Override
    public LivingEntity getHiveMindTarget() {
        return this.hiveMindTarget;
    }

    @Override
    public boolean hasAttackTarget() {
        return this.getTarget() != null && this.getTarget().isAlive();
    }

    @Override
    public int getEvolutionLevel() {
        return this.evolutionLevel;
    }
    
    public void setCorpseHunger(int value) {
        this.corpseHunger = Math.min(MAX_CORPSE_HUNGER, Math.max(0, value));
        
        if (this.corpseHunger >= MAX_CORPSE_HUNGER) {
            onCorpseHungerFull();
        }
    }
    
    public void addCorpseHunger(int amount) {
        setCorpseHunger(this.corpseHunger + amount);
    }
    
    /**
     * 当饱腹值达到3时触发等级提升
     */
    private void onCorpseHungerFull() {
        if (this.evolutionLevel < 5) {
            evolve();
            CorpseOrigin.LOGGER.info("尸兄鱼 {} 因吞噬尸体饱腹值满而进化到 {} 级！", this.getId(), this.evolutionLevel);
        }
        this.corpseHunger = 0;
    }
    
    /**
     * 进化
     */
    private void evolve() {
        this.evolutionLevel++;
        
        double healthBonus = this.evolutionLevel * 2.0;
        double damageBonus = this.evolutionLevel * 0.5;
        
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0 + healthBonus);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0 + damageBonus);
        
        this.setHealth(this.getMaxHealth());
        
        this.playSound(net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.5F, 0.8F);
        
        CorpseOrigin.LOGGER.info("尸兄鱼进化到 {} 级！", this.evolutionLevel);
    }
    
    /**
     * 吞噬尸体实体
     */
    public void eatCorpseGib(CorpseGibEntity gib) {
        if (this.level().isClientSide) return;
        
        gib.discard();
        addCorpseHunger(1);
        
        this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                this.getX(), this.getY() + 0.5, this.getZ(),
                5, 0.3, 0.3, 0.3, 0.1);
        }
        
        CorpseOrigin.LOGGER.info("尸兄鱼 {} 吞噬了尸体，饱腹值: {}", this.getId(), this.corpseHunger);
    }
    
    /**
     * 尝试吞噬附近的尸体实体
     */
    private void tryEatNearbyCorpseGib() {
        if (this.level().isClientSide) return;
        if (this.corpseHunger >= MAX_CORPSE_HUNGER) return;
        
        AABB searchBox = this.getBoundingBox().inflate(2.0D);
        List<CorpseGibEntity> nearbyGibs = this.level().getEntitiesOfClass(CorpseGibEntity.class, searchBox);
        
        if (!nearbyGibs.isEmpty()) {
            CorpseGibEntity nearestGib = null;
            double nearestDist = Double.MAX_VALUE;
            
            for (CorpseGibEntity gib : nearbyGibs) {
                double dist = this.distanceToSqr(gib);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestGib = gib;
                }
            }
            
            if (nearestGib != null) {
                eatCorpseGib(nearestGib);
            }
        }
    }
    
    /**
     * 喷射酸液攻击
     */
    public void sprayAcidAtTarget(LivingEntity target) {
        if (this.level().isClientSide) return;
        if (this.corpseHunger < 1) return;
        if (this.acidSprayCooldown > 0) return;
        
        double distance = this.distanceTo(target);
        if (distance > 8.0D) return;
        
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
        
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 start = this.position().add(0, 0.5, 0);
            Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 direction = end.subtract(start).normalize();
            
            for (int i = 0; i < 10; i++) {
                double t = i / 10.0;
                double x = start.x + direction.x * distance * t;
                double y = start.y + direction.y * distance * t;
                double z = start.z + direction.z * distance * t;
                
                serverLevel.sendParticles(ParticleTypes.DRIPPING_HONEY,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
        
        this.acidSprayCooldown = ACID_SPRAY_COOLDOWN_TICKS;
        
        CorpseOrigin.LOGGER.info("尸兄鱼 {} 向 {} 喷射了酸液", this.getId(), target.getName().getString());
    }
}