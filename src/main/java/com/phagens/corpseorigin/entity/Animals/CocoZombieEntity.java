package com.phagens.corpseorigin.entity.Animals;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.entity.npc.UncleEntity;
import com.phagens.corpseorigin.entity.zbrs.CocoZombieXEntity;
import com.phagens.corpseorigin.player.PlayerCorpseData;
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
 * 企鹅尸兄实体
 * 使用geo模型，拥有企鹅外观的尸兄
 * 继承普通企鹅的部分特性（游泳、潜水、吃鱼）
 * 攻击时触发eat动画
 */
public class CocoZombieEntity extends PathfinderMob implements GeoEntity, ICorpseBrother, ICorpseHunger {

    // 在字段区域添加
    private boolean isFusing = false;
    private boolean hasFused = false;

    // ==================== 动画定义 ====================
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("walk"); // 使用walk作为游泳
    private static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_EAT =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_EAT_TICKS =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EAT_COOLDOWN =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);

    // ==================== 字段 ====================
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);

    private int kills = 0;
    private int eatAnimationTicks = 0;
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    // 氧气系统（继承自普通企鹅）
    private static final int MAX_AIR = 300;
    private static final int OXYGEN_WARNING_LEVEL = 30;
    private int surfaceCooldown = 0;

    // 主人系统
    private static final double FOLLOW_RANGE = 16.0D;
    private static final double TELEPORT_RANGE = 32.0D;
    private int followCooldown = 0;

    // 饥饿系统（独立于CorpseHungerSystem的普通饥饿，用于消化机制）
    private int regularHunger = 720;
    private static final int MAX_REGULAR_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1200;
    private int hungerDrainTimer = 0;

    // 导航
    private final WaterBoundPathNavigation waterNavigation;
    private final GroundPathNavigation groundNavigation;

    // ==================== 构造函数 ====================
    public CocoZombieEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        // 设置路径优先级
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);

        // 创建水陆导航
        this.waterNavigation = new WaterBoundPathNavigation(this, level);
        this.groundNavigation = new GroundPathNavigation(this, level);

        // 初始使用地面导航
        this.navigation = groundNavigation;
    }

    public CocoZombieEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player player) {
        this(entityType, level);
        if (player != null) {
            hungerSystem.setMasterUUID(player.getUUID());
            CorpseOrigin.LOGGER.info("企鹅尸兄感染玩家: {}", player.getName().getString());
        }
    }

    // ==================== 属性 ====================
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
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
    }

    // ==================== AI目标 ====================
    @Override
    protected void registerGoals() {
        // 最高优先级：攻击大叔（为了合体！）
        // 移除 hasDroppedWorm() 的限制！无论大叔是否掉落虫都要攻击
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, UncleEntity.class,
                10, true, false,
                entity -> entity instanceof UncleEntity && ((UncleEntity) entity).isAlive()));
        // 行为目标
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // 攻击目标
        // 第1优先级：主动攻击非尸兄玩家（普通玩家）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::shouldAttackNonCorpsePlayer));
        // 第2优先级：攻击村民
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        // 第3优先级：攻击动物（除了企鹅）
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false,
                entity -> !(entity instanceof CocoZombieEntity) && !(entity instanceof CocoPenguinEntity)));
        // 第4优先级：攻击鱼类
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractFish.class, 10, true, false,
                entity -> entity instanceof AbstractFish));

        // ========== 反击同类（只在被攻击后反击） ==========
        // 第5优先级：反击尸兄玩家（被攻击后才会触发）
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::shouldAttackCorpsePlayer));
        // 第6优先级：反击其他尸兄实体（被攻击后才会触发）
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, CocoZombieEntity.class, 10, true, false, this::shouldAttackOtherCorpseEntity));
    }

    // ==================== 攻击判定 ====================

    /**
     * 判断是否应该攻击非尸兄玩家（普通玩家）- 主动攻击
     */
    private boolean shouldAttackNonCorpsePlayer(LivingEntity entity) {
        return hungerSystem.shouldAttackNonCorpsePlayer(entity);
    }

    /**
     * 判断是否应该攻击尸兄玩家（被攻击后反击）
     */
    private boolean shouldAttackCorpsePlayer(LivingEntity entity) {
        return hungerSystem.shouldAttackCorpsePlayer(entity);
    }

    /**
     * 判断是否应该攻击其他尸兄实体（被攻击后反击）
     */
    private boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        // 不反击自己
        if (entity == this) return false;
        // 不反击普通企鹅
        if (entity instanceof CocoPenguinEntity) return false;
        return hungerSystem.shouldAttackOtherCorpseEntity(entity);
    }

    // ==================== 攻击逻辑 ====================
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // 触发eat动画
        triggerEatAnimation();

        boolean result = super.doHurtTarget(target);

        if (result && !this.level().isClientSide) {
            // 播放吃音效
            float pitch = 0.8F + this.random.nextFloat() * 0.4F;
            this.playSound(ModSounds.GROUND_CHI.get(), 1.0F, pitch);

            // 攻击粒子效果
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        1, 0.1, 0.1, 0.1, 0);
            }

            // 击杀后处理
            handleKill(target);
        }

        return result;
    }

    /**
     * 触发eat动画
     */
    private void triggerEatAnimation() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_EAT, true);
        this.entityData.set(DATA_EAT_TICKS, 45);
        this.eatAnimationTicks = 45;
        this.entityData.set(DATA_EAT_COOLDOWN, 20);

        // 停止移动
        this.getNavigation().stop();

//        CorpseOrigin.LOGGER.debug("企鹅尸兄 {} 触发eat动画", this.getId());
    }

    /**
     * 处理击杀逻辑
     */
    private void handleKill(net.minecraft.world.entity.Entity target) {
        // 增加击杀计数
        this.kills++;

        // 恢复生命值
        if (target instanceof LivingEntity livingEntity) {
            float targetHealth = livingEntity.getMaxHealth();
            this.heal(targetHealth * 0.2F);
        }

        // 增加尸兄饱腹值
        addCorpseHunger(1);

        // 增加普通饥饿值
        addRegularHunger(20);

        // 根据目标类型额外增加
        if (target instanceof Salmon) addRegularHunger(30);
        else if (target instanceof Cod) addRegularHunger(25);
        else if (target instanceof TropicalFish) addRegularHunger(15);
        else if (target instanceof Pufferfish) addRegularHunger(10);

        // 检查进化
        checkEvolution();

        // 在 handleKill(Entity target) 方法末尾添加
// 击杀大叔时触发合体进化！
        if (target instanceof UncleEntity uncle && !hasFused) {
            tryFuseWithUncle(uncle);
        }
    }

    /**
     * 与大叔尸体合体，进化为 CocoZombieXEntity
     * 原著剧情：CoCo 用长舌贯穿大叔头部后，与大叔尸体合体
     */
    private void tryFuseWithUncle(UncleEntity uncle) {
        if (this.level().isClientSide) return;
        if (isFusing || hasFused) return;
        this.isFusing = true;

        CorpseOrigin.LOGGER.info("企鹅尸兄击杀了大叔！开始合体进化...");

        // 播放击杀特效（长舌贯穿）
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 tongueVec = uncle.position().add(0, uncle.getBbHeight() / 2, 0);
            for (int i = 0; i < 30; i++) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        tongueVec.x, tongueVec.y, tongueVec.z,
                        1, 0.1, 0.1, 0.1, 0);
            }
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    uncle.getX(), uncle.getY() + uncle.getBbHeight() / 2, uncle.getZ(),
                    20, 0.3, 0.3, 0.3, 0.1);
        }

        // 播放合体音效
        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.5F, 0.6F);
        this.playSound(ModSounds.GROUND_CHI.get(), 1.5F, 0.5F);

        // 创建合体实体
        CocoZombieXEntity fusedEntity = new CocoZombieXEntity(this.level(), this, uncle);

        // 移除原实体
        this.discard();
        uncle.discard();

        // 生成合体实体
        fusedEntity.setPos(this.getX(), this.getY(), this.getZ());
        this.level().addFreshEntity(fusedEntity);

        // 全屏警告
        if (this.level() instanceof ServerLevel serverLevel) {
            for (Player player : serverLevel.players()) {
                if (player.distanceTo(fusedEntity) < 50) {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§c§l⚠ 企鹅尸兄击杀了大叔！正在合体进化！ ⚠"
                            )
                    );
                }
            }
        }

        this.hasFused = true;
        CorpseOrigin.LOGGER.info("企鹅尸兄与大叔合体完成！生成 CocoZombieXEntity");
    }

    // ==================== 进化系统 ====================
    private void checkEvolution() {
        int requiredKills = this.getEvolutionLevel() * 8;

        if (this.kills >= requiredKills && this.getEvolutionLevel() < 5) {
            evolve();
        }
    }

    private void evolve() {
        this.setEvolutionLevel(this.getEvolutionLevel() + 1);
        this.kills = 0;

        // 增加属性
        double healthBonus = this.getEvolutionLevel() * 5.0;
        double damageBonus = this.getEvolutionLevel() * 1.0;
        double speedBonus = this.getEvolutionLevel() * 0.03;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(35.0 + healthBonus);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0 + damageBonus);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28 + speedBonus);
        this.getAttribute(Attributes.ARMOR).setBaseValue(2.0 + this.getEvolutionLevel());

        this.setHealth(this.getMaxHealth());

        // 播放进化效果
        this.level().broadcastEntityEvent(this, (byte) 20);
        this.playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.5F, 0.8F);

        CorpseOrigin.LOGGER.info("企鹅尸兄进化到 {} 级！", this.getEvolutionLevel());
    }

    // ==================== 尸体吞噬 ====================
    public void eatCorpseGib(CorpseGibEntity gib) {
        if (this.level().isClientSide) return;

        triggerEatAnimation();
        gib.discard();
        addCorpseHunger(1);
        addRegularHunger(30);

        this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    5, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private void tryEatNearbyCorpseGib() {
        if (this.level().isClientSide) return;
        if (hungerSystem.getCorpseHunger() >= 100) return;

        AABB searchBox = this.getBoundingBox().inflate(2.0D);
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

    // ==================== 喂养系统（继承自普通企鹅） ====================
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

        // 同时恢复生命值
        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(amount / 5.0F);
        }
    }

    public int getRegularHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    // ==================== 氧气系统（继承自普通企鹅） ====================
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
                // 水面呼吸
                int air = this.entityData.get(DATA_AIR_SUPPLY);
                if (air < MAX_AIR) {
                    this.entityData.set(DATA_AIR_SUPPLY, Math.min(MAX_AIR, air + 12));
                }
            }
        } else {
            // 离开水中恢复氧气
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
            // 向上游
            this.setDeltaMovement(this.getDeltaMovement().x, 0.4, this.getDeltaMovement().z);
            this.hasImpulse = true;
            surfaceCooldown = 60;
        }
    }

    // ==================== 普通饥饿系统 ====================
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

    // ==================== 动画控制器 ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationController));
    }

    private <E extends CocoZombieEntity> PlayState animationController(AnimationState<E> event) {
        // 优先播放eat动画
        if (this.entityData.get(DATA_PLAYING_EAT)) {
            return event.setAndContinue(EAT_ANIM);
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
        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 动画状态更新 ====================
    private void tickEatAnimation() {
        if (this.level().isClientSide) return;

        if (this.entityData.get(DATA_PLAYING_EAT)) {
            int eatTicks = this.entityData.get(DATA_EAT_TICKS);
            eatTicks--;
            this.entityData.set(DATA_EAT_TICKS, eatTicks);

            if (eatTicks <= 0) {
                this.entityData.set(DATA_PLAYING_EAT, false);
            }
        }

        int eatCooldown = this.entityData.get(DATA_EAT_COOLDOWN);
        if (eatCooldown > 0) {
            this.entityData.set(DATA_EAT_COOLDOWN, eatCooldown - 1);
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
            this.getNavigation().moveTo(master, 1.2D);
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
        // 记录被攻击（用于反击）
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

        // 更新导航（水陆切换）
        updateNavigation();

        // 攻击冷却
        if (attackCooldown > 0) attackCooldown--;

        if (!this.level().isClientSide) {
            // 动画状态
            tickEatAnimation();

            // 尸兄饥饿系统
            hungerSystem.tick();

            // 普通饥饿系统
            handleRegularHungerDrain();

            // 氧气系统
            handleAirSupply();
            handleSurfaceForAir();

            // 更新导航（根据是否在水中切换）
            updateNavigation();

            // 吞噬尸体
            if (this.tickCount % 40 == 0) {
                tryEatNearbyCorpseGib();
            }

            // 跟随主人
            tickFollowMaster();

            // 视力范围
            updateVisionRange();

            // 攻击逻辑
            if (this.getTarget() != null && this.getTarget().isAlive() && this.distanceTo(this.getTarget()) < 2.0D) {
                if (attackCooldown <= 0) {
                    doHurtTarget(this.getTarget());
                    attackCooldown = ATTACK_COOLDOWN_TICKS;
                }
            }
        }
    }

    private void updateVisionRange() {
        double baseVision = 16.0D;
        double levelBonus = this.getEvolutionLevel() * 2.0D;
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
        if (hungerSystem.getMasterUUID() != null) {
            compound.putUUID("MasterUUID", hungerSystem.getMasterUUID());
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
        if (compound.contains("MasterUUID")) {
            hungerSystem.setMasterUUID(compound.getUUID("MasterUUID"));
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

        // 喂养
        if (feedFish(itemStack)) {
            return InteractionResult.SUCCESS;
        }

        // 主人交互显示信息
        if (hasMaster() && getMasterUUID().equals(player.getUUID())) {
            if (!this.level().isClientSide) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§e企鹅尸兄 - 等级: " + getEvolutionLevel() +
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

        // 掉落普通尸眼
        if (this.random.nextFloat() < 0.5f) {
            this.spawnAtLocation(new ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
        }

        // 高等级额外掉落
        if (this.getEvolutionLevel() >= 3 && this.random.nextFloat() < 0.3f) {
            this.spawnAtLocation(new ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
        }

        // 有概率掉落企鹅肉（如果有这个物品）
        if (this.random.nextFloat() < 0.4f) {
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.COD), 0.0f);
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

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY);
    }

    public int getMaxAir() {
        return MAX_AIR;
    }

}