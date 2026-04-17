package com.phagens.corpseorigin.entity.zbrs;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import com.phagens.corpseorigin.entity.Animals.CocoZombieEntity;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import com.phagens.corpseorigin.entity.npc.UncleEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import com.phagens.corpseorigin.register.ModSounds;
import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * 企鹅尸兄二阶段 - CoCo与大叔的合体尸兄
 *
 * 原著设定：CoCo吞食变异虫后异变，与主人"叔"的尸体合体
 *
 * 模型说明：
 * - 使用 coco_penguin_zbrx.geo.json 模型（已包含企鹅+大叔合体结构）
 * - 触手作为独立实体（TentacleEntity）附加到此实体上
 *
 * 特点：
 * - 双实体合体外观（企鹅+大叔）
 * - 触手攻击（长舌/藤蔓）- 通过附加的触手实体实现
 * - 更强的属性
 */
public class CocoZombieXEntity extends PathfinderMob implements GeoEntity, ICorpseBrother, ICorpseHunger {

    // ==================== 动画定义（完整修复） ====================
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("walk"); // 复用walk作为游泳
    private static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");
    private static final RawAnimation FUSION_IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation TONGUE_ATTACK_ANIM = RawAnimation.begin().thenPlayAndHold("eat");

    // 新增：缺失的动画定义
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ATTACKX_ANIM = RawAnimation.begin().thenPlay("attackx");
    private static final RawAnimation HYPOCRISY_ANIM = RawAnimation.begin().thenPlay("hypocrisy");
    private static final RawAnimation DROP_ANIM = RawAnimation.begin().thenPlayAndHold("drop");

    // 合体特殊动画
    private static final RawAnimation FUSION_COMPLETE_ANIM = RawAnimation.begin().thenPlay("hypocrisy"); // 使用hypocrisy作为合体完成动画
    private static final RawAnimation TENTACLE_SPAWN_ANIM = RawAnimation.begin().thenPlay("attackx"); // 触手生成动画

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_EAT =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_EAT_TICKS =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EAT_COOLDOWN =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);

    // 合体特有数据
    private static final EntityDataAccessor<Boolean> DATA_TONGUE_ATTACKING =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TENTACLE_COUNT =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUSION_TIER =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);

    // 新增：动画状态同步
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_ATTACK =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_HYPOCRISY =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FUSION_COMPLETE =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);

    // ==================== 字段 ====================
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);

    private int kills = 0;
    private int eatAnimationTicks = 0;
    private int attackCooldown = 0;
    private int tongueAttackCooldown = 0;
    private int attackAnimationTicks = 0;
    private int hypocrisyAnimationTicks = 0;

    private static final int ATTACK_COOLDOWN_TICKS = 15;
    private static final int TONGUE_COOLDOWN_TICKS = 40;
    private static final int TONGUE_RANGE = 12;
    private static final int ATTACK_ANIMATION_DURATION = 10; // attack动画时长约0.5秒 = 10ticks
    private static final int HYPOCRISY_ANIMATION_DURATION = 60; // hypocrisy动画时长约2.94秒 = 60ticks

    // 氧气系统
    private static final int MAX_AIR = 400;
    private static final int OXYGEN_WARNING_LEVEL = 30;
    private int surfaceCooldown = 0;

    // 主人系统
    private static final double FOLLOW_RANGE = 20.0D;
    private static final double TELEPORT_RANGE = 40.0D;
    private int followCooldown = 0;

    // 饥饿系统
    private int regularHunger = 720;
    private static final int MAX_REGULAR_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1000;
    private int hungerDrainTimer = 0;

    // 合体特有字段
    private UUID uncleUUID;           // 合体的大叔UUID
    private List<Entity> attachedTentacles; // 附加的触手实体
    private LivingEntity tongueTarget = null;

    // 合体状态标记
    private boolean isFusing = false;
    private boolean hasFused = false;
    private int fusionAnimationTimer = 0;

    // 导航
    private final WaterBoundPathNavigation waterNavigation;
    private final GroundPathNavigation groundNavigation;

    // ==================== 构造函数 ====================
    public CocoZombieXEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);

        this.waterNavigation = new WaterBoundPathNavigation(this, level);
        this.groundNavigation = new GroundPathNavigation(this, level);
        this.navigation = groundNavigation;
    }

    public CocoZombieXEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player player) {
        this(entityType, level);
        if (player != null) {
            hungerSystem.setMasterUUID(player.getUUID());
            CorpseOrigin.LOGGER.info("合体企鹅尸兄感染玩家: {}", player.getName().getString());
        }
    }

    /**
     * 从一阶段企鹅尸兄和大叔合体创建
     */
    public CocoZombieXEntity(Level level, CocoZombieEntity cocoZombie, UncleEntity uncle) {
        this(EntityRegistry.COCO_ZOMBIE_X.get(), level);

        // 继承一阶段属性
        this.setPos(cocoZombie.getX(), cocoZombie.getY(), cocoZombie.getZ());
        this.kills = cocoZombie.getKills();
        this.setEvolutionLevel(cocoZombie.getEvolutionLevel());
        this.setCorpseHunger(cocoZombie.getCorpseHunger());
        this.setRegularHunger(cocoZombie.getRegularHunger());

        if (cocoZombie.hasMaster()) {
            this.setMaster(cocoZombie.getMasterUUID());
        }

        // 记录合体的大叔
        if (uncle != null) {
            this.uncleUUID = uncle.getUUID();
        }

        // 合体特效
        this.setFusionTier(1);
        this.entityData.set(DATA_TENTACLE_COUNT, 4);
        this.setHealth(this.getMaxHealth());

        // 播放合体完成动画
        this.playFusionCompleteAnimation();
        this.level().broadcastEntityEvent(this, (byte) 20);

        CorpseOrigin.LOGGER.info("企鹅尸兄与大叔合体完成！进化等级: {}, 触手数量: {}",
                this.getEvolutionLevel(), this.entityData.get(DATA_TENTACLE_COUNT));
    }

    /**
     * 播放合体完成动画
     */
    private void playFusionCompleteAnimation() {
        this.entityData.set(DATA_FUSION_COMPLETE, true);
        this.entityData.set(DATA_PLAYING_HYPOCRISY, true);
        this.hypocrisyAnimationTicks = HYPOCRISY_ANIMATION_DURATION;
        this.isFusing = true;
        this.fusionAnimationTimer = 60; // 3秒合体动画

        // 播放合体音效
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);
    }

    // ==================== 属性（合体后强化） ====================
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    // ==================== 数据同步 ====================
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYING_EAT, false);
        builder.define(DATA_EAT_TICKS, 0);
        builder.define(DATA_EAT_COOLDOWN, 0);
        builder.define(DATA_HUNGER, 720);
        builder.define(DATA_AIR_SUPPLY, MAX_AIR);
        builder.define(DATA_TONGUE_ATTACKING, false);
        builder.define(DATA_TENTACLE_COUNT, 4);
        builder.define(DATA_FUSION_TIER, 1);
        // 新增动画状态
        builder.define(DATA_PLAYING_ATTACK, false);
        builder.define(DATA_PLAYING_HYPOCRISY, false);
        builder.define(DATA_FUSION_COMPLETE, false);
    }

    // ==================== AI目标（更激进） ====================
    @Override
    protected void registerGoals() {
        // 行为目标
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // 攻击目标
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::shouldAttackNonCorpsePlayer));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false,
                entity -> !(entity instanceof CocoZombieXEntity) && !(entity instanceof CocoPenguinEntity)));

        // 反击目标
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::shouldAttackCorpsePlayer));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, CocoZombieXEntity.class, 10, true, false, this::shouldAttackOtherCorpseEntity));
    }

    // ==================== 攻击判定 ====================
    private boolean shouldAttackNonCorpsePlayer(LivingEntity entity) {
        return hungerSystem.shouldAttackNonCorpsePlayer(entity);
    }

    private boolean shouldAttackCorpsePlayer(LivingEntity entity) {
        return hungerSystem.shouldAttackCorpsePlayer(entity);
    }

    private boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        if (entity == this) return false;
        if (entity instanceof CocoPenguinEntity) return false;
        return hungerSystem.shouldAttackOtherCorpseEntity(entity);
    }

    // ==================== 舌头攻击（远程穿透） ====================
    public void performTongueAttack() {
        if (tongueAttackCooldown > 0 || this.level().isClientSide) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = this.distanceTo(target);
        if (distance > TONGUE_RANGE) return;

        // 触发舌头动画（复用eat动画）
        triggerEatAnimation();
        this.entityData.set(DATA_TONGUE_ATTACKING, true);
        tongueAttackCooldown = TONGUE_COOLDOWN_TICKS;
        tongueTarget = target;

        // 造成伤害
        float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5);
        boolean hurt = target.hurt(this.damageSources().mobAttack(this), damage);

        if (hurt) {
            // 击退效果
            Vec3 knockback = target.position().subtract(this.position()).normalize().scale(1.2);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));

            // 音效和粒子
            this.playSound(SoundEvents.FOX_TELEPORT, 1.5F, 0.6F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.05);
            }

            CorpseOrigin.LOGGER.debug("合体尸兄使用舌头攻击: {}", target.getName().getString());
        }
    }

    /**
     * 藤蔓缠绕（控制技能）
     */
    private void applyVineSnare(LivingEntity target) {
        if (target == null) return;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));

        // 粒子效果
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // ==================== 基础攻击 ====================
    @Override
    public boolean doHurtTarget(Entity target) {
        triggerEatAnimation();

        // 触发攻击动画
        triggerAttackAnimation();

        boolean result = super.doHurtTarget(target);

        if (result && !this.level().isClientSide) {
            float pitch = 0.7F + this.random.nextFloat() * 0.4F;
            this.playSound(ModSounds.GROUND_CHI.get(), 1.2F, pitch);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        2, 0.2, 0.2, 0.2, 0);
            }

            // 合体后攻击附带缠绕效果
            if (target instanceof LivingEntity livingTarget && this.random.nextFloat() < 0.3f) {
                applyVineSnare(livingTarget);
            }

            handleKill(target);
        }

        return result;
    }

    /**
     * 触发攻击动画
     */
    private void triggerAttackAnimation() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_ATTACK, true);
        this.attackAnimationTicks = ATTACK_ANIMATION_DURATION;
    }

    /**
     * 触发 hypocrisy 动画（合体特殊动作）
     */
    private void triggerHypocrisyAnimation() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_HYPOCRISY, true);
        this.hypocrisyAnimationTicks = HYPOCRISY_ANIMATION_DURATION;
    }

    private void triggerEatAnimation() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_EAT, true);
        this.entityData.set(DATA_EAT_TICKS, 35);
        this.eatAnimationTicks = 35;
        this.entityData.set(DATA_EAT_COOLDOWN, 15);
        this.getNavigation().stop();
    }

    private void handleKill(Entity target) {
        this.kills++;

        if (target instanceof LivingEntity livingEntity) {
            float targetHealth = livingEntity.getMaxHealth();
            this.heal(targetHealth * 0.25F);
        }

        addCorpseHunger(2);
        addRegularHunger(30);

        checkEvolution();
    }

    // ==================== 进化系统 ====================
    private void checkEvolution() {
        int requiredKills = this.getEvolutionLevel() * 10;

        if (this.kills >= requiredKills && this.getEvolutionLevel() < 8) {
            evolve();
        }
    }

    private void evolve() {
        this.setEvolutionLevel(this.getEvolutionLevel() + 1);
        this.kills = 0;

        // 属性成长
        double healthBonus = this.getEvolutionLevel() * 6.0;
        double damageBonus = this.getEvolutionLevel() * 1.5;
        double speedBonus = this.getEvolutionLevel() * 0.04;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(65.0 + healthBonus);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(9.0 + damageBonus);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32 + speedBonus);
        this.getAttribute(Attributes.ARMOR).setBaseValue(6.0 + this.getEvolutionLevel());

        this.setHealth(this.getMaxHealth());

        // 进化时增加触手数量
        int newTentacleCount = Math.min(8, 4 + this.getEvolutionLevel() / 2);
        this.entityData.set(DATA_TENTACLE_COUNT, newTentacleCount);

        // 播放触手生成动画
        triggerHypocrisyAnimation();

        // 合体阶段提升
        if (this.getEvolutionLevel() >= 3) {
            this.setFusionTier(2);
        }
        if (this.getEvolutionLevel() >= 6) {
            this.setFusionTier(3);
        }

        // 特效
        this.level().broadcastEntityEvent(this, (byte) 20);
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);

        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        1, 0.5, 0.5, 0.5, 0.05);
            }
        }

        CorpseOrigin.LOGGER.info("合体企鹅尸兄进化到 {} 级！合体阶段: {}, 触手数量: {}",
                this.getEvolutionLevel(), this.getFusionTier(), newTentacleCount);
    }

    // ==================== 吞噬尸体 ====================
    public void eatCorpseGib(CorpseGibEntity gib) {
        if (this.level().isClientSide) return;

        triggerEatAnimation();
        gib.discard();
        addCorpseHunger(2);
        addRegularHunger(40);

        this.playSound(SoundEvents.GENERIC_EAT, 1.2F, 0.7F + this.random.nextFloat() * 0.4F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    8, 0.4, 0.4, 0.4, 0.1);
        }
    }

    private void tryEatNearbyCorpseGib() {
        if (this.level().isClientSide) return;
        if (hungerSystem.getCorpseHunger() >= 100) return;

        AABB searchBox = this.getBoundingBox().inflate(2.5D);
        List<CorpseGibEntity> nearbyGibs = this.level().getEntitiesOfClass(CorpseGibEntity.class, searchBox);

        if (!nearbyGibs.isEmpty()) {
            CorpseGibEntity nearestGib = nearbyGibs.stream()
                    .min((a, b) -> Double.compare(this.distanceToSqr(a), this.distanceToSqr(b)))
                    .orElse(null);
            if (nearestGib != null) {
                eatCorpseGib(nearestGib);
            }
        }
    }

    // ==================== 喂养系统 ====================
    public boolean feedFish(ItemStack stack) {
        if (!canEat()) return false;

        int restoreAmount = 0;
        net.minecraft.world.item.Item item = stack.getItem();

        if (item == net.minecraft.world.item.Items.SALMON) {
            restoreAmount = 40;
        } else if (item == net.minecraft.world.item.Items.COD) {
            restoreAmount = 35;
        } else if (item == net.minecraft.world.item.Items.TROPICAL_FISH) {
            restoreAmount = 25;
        } else if (item == net.minecraft.world.item.Items.PUFFERFISH) {
            restoreAmount = 15;
        } else if (item == Moditems.ZB_WORM_ITEM.get()) {
            restoreAmount = 30;
        } else {
            return false;
        }

        addRegularHunger(restoreAmount);

        if (!this.isInWater()) {
            triggerEatAnimation();
        }

        if (!this.level().isClientSide) {
            stack.shrink(1);
        }

        return true;
    }

    private boolean canEat() {
        return this.entityData.get(DATA_EAT_COOLDOWN) <= 0;
    }

    public void addRegularHunger(int amount) {
        int newHunger = Math.min(MAX_REGULAR_HUNGER, getRegularHunger() + amount);
        this.entityData.set(DATA_HUNGER, newHunger);

        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(amount / 5.0F);
        }
    }

    public int getRegularHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setRegularHunger(int hunger) {
        this.entityData.set(DATA_HUNGER, Math.min(MAX_REGULAR_HUNGER, Math.max(0, hunger)));
    }

    // ==================== 氧气系统 ====================
    private void handleAirSupply() {
        if (this.isInWater()) {
            if (this.isUnderWater()) {
                int air = this.entityData.get(DATA_AIR_SUPPLY);
                if (air > 0) {
                    this.entityData.set(DATA_AIR_SUPPLY, air - 1);
                }
                if (air <= 0) {
                    this.hurt(this.damageSources().drown(), 1.0F);
                }
            } else {
                int air = this.entityData.get(DATA_AIR_SUPPLY);
                if (air < MAX_AIR) {
                    this.entityData.set(DATA_AIR_SUPPLY, Math.min(MAX_AIR, air + 12));
                }
            }
        } else {
            int air = this.entityData.get(DATA_AIR_SUPPLY);
            if (air < MAX_AIR) {
                this.entityData.set(DATA_AIR_SUPPLY, Math.min(MAX_AIR, air + 8));
            }
        }
    }

    private void handleSurfaceForAir() {
        if (surfaceCooldown > 0) {
            surfaceCooldown--;
            return;
        }
        int air = this.entityData.get(DATA_AIR_SUPPLY);
        if (air <= OXYGEN_WARNING_LEVEL && this.isInWater() && this.isUnderWater()) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.4, this.getDeltaMovement().z);
            this.hasImpulse = true;
            surfaceCooldown = 60;
        }
    }

    // ==================== 饥饿系统 ====================
    private void handleRegularHungerDrain() {
        if (this.level().isClientSide) return;

        hungerDrainTimer++;
        if (hungerDrainTimer >= HUNGER_DRAIN_INTERVAL) {
            hungerDrainTimer = 0;
            int newHunger = Math.max(0, getRegularHunger() - 1);
            this.entityData.set(DATA_HUNGER, newHunger);

            if (newHunger <= 0) {
                this.hurt(this.damageSources().starve(), 1.0F);
            }
        }
    }

    // ==================== 动画控制器（完整修复） ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主动画控制器
        controllers.add(new AnimationController<>(this, "mainController", 5, this::mainAnimationController));

        // 攻击动画控制器（独立，优先级更高）
        controllers.add(new AnimationController<>(this, "attackController", 3, this::attackAnimationController));

        // 合体动画控制器
        controllers.add(new AnimationController<>(this, "fusionController", 2, this::fusionAnimationController));

        // 舌头攻击动画控制器
        controllers.add(new AnimationController<>(this, "tongueController", 4, this::tongueAnimationController));
    }

    /**
     * 主动画控制器 - 处理待机、行走、游泳
     */
    private <E extends CocoZombieXEntity> PlayState mainAnimationController(AnimationState<E> event) {
        // 如果正在播放攻击动画，不干扰
        if (this.entityData.get(DATA_PLAYING_ATTACK)) {
            return PlayState.CONTINUE;
        }

        // 如果正在播放合体动画
        if (this.entityData.get(DATA_PLAYING_HYPOCRISY) || this.entityData.get(DATA_FUSION_COMPLETE)) {
            return PlayState.CONTINUE;
        }

        // 如果正在播放舌头攻击动画
        if (this.entityData.get(DATA_PLAYING_EAT)) {
            return PlayState.CONTINUE;
        }

        // 水中播放游泳动画
        if (this.isInWater()) {
            return event.setAndContinue(SWIM_ANIM);
        }

        // 移动时播放行走动画
        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }

        // 默认待机动画
        return event.setAndContinue(FUSION_IDLE_ANIM);
    }

    /**
     * 攻击动画控制器
     */
    private <E extends CocoZombieXEntity> PlayState attackAnimationController(AnimationState<E> event) {
        if (this.entityData.get(DATA_PLAYING_ATTACK)) {
            return event.setAndContinue(ATTACK_ANIM);
        }
        return PlayState.STOP;
    }

    /**
     * 合体动画控制器
     */
    private <E extends CocoZombieXEntity> PlayState fusionAnimationController(AnimationState<E> event) {
        // 播放合体完成动画
        if (this.entityData.get(DATA_FUSION_COMPLETE)) {
            return event.setAndContinue(FUSION_COMPLETE_ANIM);
        }

        // 播放 hypocrisy 动画
        if (this.entityData.get(DATA_PLAYING_HYPOCRISY)) {
            return event.setAndContinue(HYPOCRISY_ANIM);
        }

        return PlayState.STOP;
    }

    /**
     * 舌头攻击动画控制器
     */
    private <E extends CocoZombieXEntity> PlayState tongueAnimationController(AnimationState<E> event) {
        if (this.entityData.get(DATA_PLAYING_EAT)) {
            return event.setAndContinue(TONGUE_ATTACK_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 动画状态更新 ====================
    private void tickAnimations() {
        tickEatAnimation();
        tickAttackAnimation();
        tickHypocrisyAnimation();
        tickFusionAnimation();
    }

    private void tickEatAnimation() {
        if (this.level().isClientSide) return;

        if (this.entityData.get(DATA_PLAYING_EAT)) {
            int eatTicks = this.entityData.get(DATA_EAT_TICKS);
            eatTicks--;
            this.entityData.set(DATA_EAT_TICKS, eatTicks);

            // 舌头攻击时，在特定帧造成伤害
            if (this.entityData.get(DATA_TONGUE_ATTACKING) && eatTicks == 25) {
                if (tongueTarget != null && tongueTarget.isAlive() && this.distanceTo(tongueTarget) <= TONGUE_RANGE) {
                    float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5);
                    tongueTarget.hurt(this.damageSources().mobAttack(this), damage);
                }
            }

            if (eatTicks <= 0) {
                this.entityData.set(DATA_PLAYING_EAT, false);
                this.entityData.set(DATA_TONGUE_ATTACKING, false);
                tongueTarget = null;
            }
        }

        int eatCooldown = this.entityData.get(DATA_EAT_COOLDOWN);
        if (eatCooldown > 0) {
            this.entityData.set(DATA_EAT_COOLDOWN, eatCooldown - 1);
        }
    }

    private void tickAttackAnimation() {
        if (this.level().isClientSide) return;

        if (this.entityData.get(DATA_PLAYING_ATTACK)) {
            this.attackAnimationTicks--;
            if (this.attackAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_ATTACK, false);
            }
        }
    }

    private void tickHypocrisyAnimation() {
        if (this.level().isClientSide) return;

        if (this.entityData.get(DATA_PLAYING_HYPOCRISY)) {
            this.hypocrisyAnimationTicks--;
            if (this.hypocrisyAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_HYPOCRISY, false);
            }
        }
    }

    private void tickFusionAnimation() {
        if (this.level().isClientSide) return;

        if (this.entityData.get(DATA_FUSION_COMPLETE)) {
            this.fusionAnimationTimer--;
            if (this.fusionAnimationTimer <= 0) {
                this.entityData.set(DATA_FUSION_COMPLETE, false);
                this.isFusing = false;
                this.hasFused = true;
            }
        }
    }

    // ==================== 导航切换 ====================
    private void updateNavigation() {
        if (this.isInWater()) {
            this.navigation = waterNavigation;
        } else {
            this.navigation = groundNavigation;
        }
    }

    // ==================== 主人跟随系统 ====================
    private void tickFollowMaster() {
        if (hungerSystem.getMasterUUID() == null) return;

        if (followCooldown > 0) {
            followCooldown--;
            return;
        }

        if (this.tickCount % 10 != 0) return;

        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Player master = serverLevel.getServer().getPlayerList().getPlayer(hungerSystem.getMasterUUID());
        if (master == null || !master.isAlive()) return;

        double distanceToMaster = this.distanceToSqr(master);

        if (distanceToMaster > TELEPORT_RANGE * TELEPORT_RANGE) {
            teleportToMaster(master);
            return;
        }

        if (distanceToMaster > FOLLOW_RANGE * FOLLOW_RANGE) {
            this.getNavigation().moveTo(master, 1.3D);
            followCooldown = 20;
        }
    }

    private void teleportToMaster(Player master) {
        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = 2 + this.random.nextDouble() * 2;
            double targetX = master.getX() + Math.cos(angle) * distance;
            double targetZ = master.getZ() + Math.sin(angle) * distance;
            double targetY = master.getY();

            if (this.level().noCollision(this.getBoundingBox().move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ()))) {
                this.teleportTo(targetX, targetY, targetZ);
                this.getNavigation().stop();
                followCooldown = 40;
                break;
            }
        }
    }

    // ==================== 水中移动 ====================
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.04F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.96D));
        } else {
            super.travel(travelVector);
        }
    }

    // ==================== 状态效果 ====================
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value() == MobEffects.POISON.value() ||
                effect.getEffect().value() == MobEffects.HUNGER.value()) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity &&
                !(source.getEntity() instanceof Player player && isMaster(player))) {
            hungerSystem.recordHurt();
        }
        return super.hurt(source, amount);
    }

    private boolean isMaster(Player player) {
        return hungerSystem.getMasterUUID() != null && player.getUUID().equals(hungerSystem.getMasterUUID());
    }

    // ==================== tick更新 ====================
    @Override
    public void aiStep() {
        super.aiStep();
        hungerSystem.tick();
    }

    @Override
    public void tick() {
        super.tick();

        updateNavigation();
        tickAnimations(); // 更新所有动画状态

        if (attackCooldown > 0) attackCooldown--;
        if (tongueAttackCooldown > 0) tongueAttackCooldown--;

        if (!this.level().isClientSide) {
            hungerSystem.tick();
            handleRegularHungerDrain();
            handleAirSupply();
            handleSurfaceForAir();
            updateNavigation();

            if (this.tickCount % 40 == 0) {
                tryEatNearbyCorpseGib();
            }

            tickFollowMaster();
            updateVisionRange();

            // 自动使用舌头攻击
            if (this.getTarget() != null && this.getTarget().isAlive() && !this.isFusing) {
                double distance = this.distanceTo(this.getTarget());
                if (distance > 2.0 && distance <= TONGUE_RANGE && tongueAttackCooldown <= 0) {
                    performTongueAttack();
                } else if (distance <= 2.0 && attackCooldown <= 0) {
                    doHurtTarget(this.getTarget());
                    attackCooldown = ATTACK_COOLDOWN_TICKS;
                }
            }
        }
    }

    private void updateVisionRange() {
        double baseVision = 20.0D;
        double levelBonus = this.getEvolutionLevel() * 2.5D;
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(baseVision + levelBonus);
        }
    }

    // ==================== 持久化 ====================
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        hungerSystem.saveData(compound);
        compound.putInt("Kills", this.kills);
        compound.putInt("RegularHunger", getRegularHunger());
        compound.putInt("AirSupply", this.entityData.get(DATA_AIR_SUPPLY));
        compound.putInt("FusionTier", this.getFusionTier());
        compound.putInt("TentacleCount", this.entityData.get(DATA_TENTACLE_COUNT));
        compound.putBoolean("HasFused", this.hasFused);
        if (hungerSystem.getMasterUUID() != null) {
            compound.putUUID("MasterUUID", hungerSystem.getMasterUUID());
        }
        if (uncleUUID != null) {
            compound.putUUID("UncleUUID", uncleUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        hungerSystem.loadData(compound);
        if (compound.contains("Kills")) {
            this.kills = compound.getInt("Kills");
        }
        if (compound.contains("RegularHunger")) {
            this.entityData.set(DATA_HUNGER, compound.getInt("RegularHunger"));
        }
        if (compound.contains("AirSupply")) {
            this.entityData.set(DATA_AIR_SUPPLY, compound.getInt("AirSupply"));
        }
        if (compound.contains("FusionTier")) {
            this.setFusionTier(compound.getInt("FusionTier"));
        }
        if (compound.contains("TentacleCount")) {
            this.entityData.set(DATA_TENTACLE_COUNT, compound.getInt("TentacleCount"));
        }
        if (compound.contains("HasFused")) {
            this.hasFused = compound.getBoolean("HasFused");
        }
        if (compound.contains("MasterUUID")) {
            hungerSystem.setMasterUUID(compound.getUUID("MasterUUID"));
        }
        if (compound.contains("UncleUUID")) {
            this.uncleUUID = compound.getUUID("UncleUUID");
        }
    }

    // ==================== ICorpseBrother 接口 ====================
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
        return this.getTarget() != null && this.getTarget().isAlive();
    }

    @Override
    public int getEvolutionLevel() {
        return hungerSystem.getEvolutionLevel();
    }

    @Override
    public void setEvolutionLevel(int level) {
        hungerSystem.setEvolutionLevel(level);
    }

    // ==================== ICorpseHunger 接口 ====================
    @Override
    public int getCorpseHunger() {
        return hungerSystem.getCorpseHunger();
    }

    @Override
    public void setCorpseHunger(int hunger) {
        hungerSystem.setCorpseHunger(hunger);
    }

    public void addCorpseHunger(int amount) {
        hungerSystem.setCorpseHunger(hungerSystem.getCorpseHunger() + amount);
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
    public BlockPos blockPosition() {
        return super.blockPosition();
    }

    @Override
    public boolean isAlive() {
        return super.isAlive();
    }

    // ==================== 合体特有方法 ====================
    public int getFusionTier() {
        return this.entityData.get(DATA_FUSION_TIER);
    }

    public void setFusionTier(int tier) {
        this.entityData.set(DATA_FUSION_TIER, Math.min(3, Math.max(1, tier)));
    }

    public int getTentacleCount() {
        return this.entityData.get(DATA_TENTACLE_COUNT);
    }

    public UUID getUncleUUID() {
        return uncleUUID;
    }

    public boolean hasFused() {
        return hasFused;
    }

    public boolean isFusing() {
        return isFusing;
    }

    // ==================== 主人系统 ====================
    public void setMaster(UUID masterUUID) {
        hungerSystem.setMasterUUID(masterUUID);
    }

    public UUID getMasterUUID() {
        return hungerSystem.getMasterUUID();
    }

    public boolean hasMaster() {
        return hungerSystem.getMasterUUID() != null;
    }

    // ==================== 交互 ====================
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack itemStack = player.getItemInHand(hand);

        if (feedFish(itemStack)) {
            return InteractionResult.SUCCESS;
        }

        if (hasMaster() && getMasterUUID().equals(player.getUUID())) {
            if (!this.level().isClientSide) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c§l合体尸兄(CoCo+大叔)§r§e - 等级: " + getEvolutionLevel() +
                                " | 合体阶段: " + getFusionTier() +
                                " | 触手: " + getTentacleCount() +
                                " | 击杀: " + this.kills +
                                " | 尸兄饱腹: " + getCorpseHunger() +
                                " | 饥饿: " + getRegularHunger()
                ));
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    // ==================== 死亡掉落 ====================
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        // 播放掉落动画
        if (!this.level().isClientSide) {
            // 死亡时播放掉落动画（通过数据标记，客户端会处理）
            this.entityData.set(DATA_PLAYING_HYPOCRISY, true);
        }

        // 掉落尸眼（更高概率）
        if (this.random.nextFloat() < 0.7f) {
            this.spawnAtLocation(new ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
        }

        // 高等级额外掉落
        if (this.getEvolutionLevel() >= 3 && this.random.nextFloat() < 0.4f) {
            this.spawnAtLocation(new ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
        }

        // 掉落大叔的遗物（如果有）
        if (this.random.nextFloat() < 0.2f) {
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.PAPER), 0.0f); // 漫画稿
        }

        // 掉落鱼肉
        if (this.random.nextFloat() < 0.5f) {
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.SALMON), 0.0f);
        }
    }

    // ==================== Getter/Setter ====================
    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public boolean isPlayingEat() {
        return this.entityData.get(DATA_PLAYING_EAT);
    }

    public boolean isPlayingAttack() {
        return this.entityData.get(DATA_PLAYING_ATTACK);
    }

    public boolean isPlayingHypocrisy() {
        return this.entityData.get(DATA_PLAYING_HYPOCRISY);
    }

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY);
    }

    public int getMaxAir() {
        return MAX_AIR;
    }
}