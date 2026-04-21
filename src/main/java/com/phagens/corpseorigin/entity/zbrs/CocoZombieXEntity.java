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
import net.minecraft.core.Position;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 企鹅尸兄二阶段 - CoCo与大叔的合体尸兄
 * <p>
 * 原著设定：CoCo吞食变异虫后异变，与主人"叔"的尸体合体
 * <p>
 * 模型说明：
 * - 使用 coco_penguin_zbrx.geo.json 模型（已包含企鹅+大叔合体结构）
 * - 触手作为独立实体（TentacleEntity）附加到此实体上
 * <p>
 * 特点：
 * - 双实体合体外观（企鹅+大叔）
 * - 触手攻击（长舌/藤蔓）- 通过附加的触手实体实现
 * - 更强的属性
 */
public class CocoZombieXEntity extends PathfinderMob implements GeoEntity, ICorpseBrother, ICorpseHunger {

    // ==================== 动画定义 - 使用动画文件中所有动画 ====================
    // 循环动画
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("walk");

    // 单次播放动画（每次攻击触发）
    private static final RawAnimation MELEE_ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");     // 近战攻击
    private static final RawAnimation TONGUE_ATTACK_ANIM = RawAnimation.begin().thenPlay("eat");       // 舌头攻击（有舌头动画）
    private static final RawAnimation BREAK_ATTACK_ANIM = RawAnimation.begin().thenPlay("attackx");    // 破窗/破门
    private static final RawAnimation HYPOCRISY_ANIM = RawAnimation.begin().thenPlay("hypocrisy");     // 合体/进化动画
    private static final RawAnimation DROP_ANIM = RawAnimation.begin().thenPlayAndHold("drop");        // 死亡掉落

    // triggerAnim 动画名称常量
    private static final String ANIM_MELEE_ATTACK = "melee_attack";
    private static final String ANIM_TONGUE_ATTACK = "tongue_attack";
    private static final String ANIM_BREAK_ATTACK = "break_attack";
    private static final String ANIM_HYPOCRISY = "hypocrisy";
    private static final String ANIM_FUSION_COMPLETE = "fusion_complete";

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TENTACLE_COUNT =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUSION_TIER =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);

    // 添加合体动画标志位
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_HYPOCRISY =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);

    // ==================== 字段 ====================
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);

    private int kills = 0;
    private int attackCooldown = 0;
    private int tongueAttackCooldown = 0;
    private int breakAttackCooldown = 0;

    private static final int ATTACK_COOLDOWN_TICKS = 15;
    private static final int TONGUE_COOLDOWN_TICKS = 40;
    private static final int BREAK_COOLDOWN_TICKS = 30;
    private static final int TONGUE_RANGE = 12;

    // 氧气系统
    private static final int MAX_AIR = 400;
    private static final int OXYGEN_WARNING_LEVEL = 30;
    private int surfaceCooldown = 0;

    // 主人系统
    private static final double FOLLOW_RANGE = 20.0D;
    private static final double TELEPORT_RANGE = 40.0D;
    private int followCooldown = 0;

    // 饥饿系统
    private static final int MAX_REGULAR_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1000;
    private int hungerDrainTimer = 0;

    // 合体特有字段
    private UUID uncleUUID;
    private LivingEntity tongueTarget = null;

    // 合体动画计时
    private int hypocrisyAnimationTicks = 0;
    private static final int HYPOCRISY_ANIMATION_DURATION = 60;  // hypocrisy 动画约2.94秒

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
        this.isFusing = true;
        this.fusionAnimationTimer = 60;
        triggerFusionCompleteAnimation();
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
        builder.define(DATA_HUNGER, 720);
        builder.define(DATA_AIR_SUPPLY, MAX_AIR);
        builder.define(DATA_TENTACLE_COUNT, 4);
        builder.define(DATA_FUSION_TIER, 1);

        builder.define(DATA_PLAYING_HYPOCRISY, false);  // 添加这行

    }

    @Override
    protected void registerGoals() {
        // 行为目标
        this.goalSelector.addGoal(0, new BreakObstacleGoal(this));  // 最高优先级，破坏障碍物
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));

        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 攻击目标（保持不变）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::shouldAttackNonCorpsePlayer));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false,
                entity -> !(entity instanceof CocoZombieXEntity) && !(entity instanceof CocoPenguinEntity)));

        // 反击目标
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::shouldAttackCorpsePlayer));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, CocoZombieXEntity.class, 10, true, false, this::shouldAttackOtherCorpseEntity));
    }

    // ==================== 破窗/破门 AI 目标 ====================
    /**
     * 破坏障碍物目标 - 当检测到房屋内有生物时，破坏阻挡的窗户和门
     */
    public static class BreakObstacleGoal extends Goal {
        private final CocoZombieXEntity entity;
        private BlockPos targetBlockPos;
        private int breakTimer = 0;
        private static final int BREAK_INTERVAL = 20;  // 每20 tick尝试破坏一次
        private static final int SEARCH_RADIUS = 15;   // 搜索半径
        private static final int MAX_BREAK_DISTANCE = 4; // 最大破坏距离

        public BreakObstacleGoal(CocoZombieXEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (entity.getTarget() == null) return false;
            LivingEntity target = entity.getTarget();
            if (!target.isAlive()) return false;

            double straightDistance = entity.distanceTo(target);
            if (straightDistance > SEARCH_RADIUS) return false;

            BlockPos entityPos = entity.blockPosition();
            BlockPos targetPos = target.blockPosition();
            targetBlockPos = findBlockedObstacle(entityPos, targetPos);

            return targetBlockPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = entity.getTarget();
            if (target == null || !target.isAlive()) return false;

            if (targetBlockPos != null && !isBreakableBlock(entity.level().getBlockState(targetBlockPos), entity.level(), targetBlockPos)) {
                targetBlockPos = findBlockedObstacle(entity.blockPosition(), target.blockPosition());
            }

            return targetBlockPos != null;
        }

        @Override
        public void start() {
            super.start();
            breakTimer = 0;
            entity.getNavigation().stop();
        }

        @Override
        public void stop() {
            super.stop();
            targetBlockPos = null;
            breakTimer = 0;
        }

        @Override
        public void tick() {
            if (targetBlockPos == null) return;

            LivingEntity target = entity.getTarget();
            if (target == null || !target.isAlive()) return;

            double distanceToBlock = entity.blockPosition().distSqr(targetBlockPos);

            if (distanceToBlock > MAX_BREAK_DISTANCE * MAX_BREAK_DISTANCE) {
                entity.getNavigation().moveTo(targetBlockPos.getX() + 0.5, targetBlockPos.getY(), targetBlockPos.getZ() + 0.5, 1.3D);
            } else {
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(targetBlockPos.getX() + 0.5, targetBlockPos.getY() + 0.5, targetBlockPos.getZ() + 0.5);

                breakTimer++;
                if (breakTimer >= BREAK_INTERVAL) {
                    entity.performBreakAttack(targetBlockPos);
                    breakTimer = 0;
                    targetBlockPos = findBlockedObstacle(entity.blockPosition(), target.blockPosition());
                }
            }
        }

        /**
         * 查找阻挡视线的可破坏方块
         */
        private BlockPos findBlockedObstacle(BlockPos from, BlockPos to) {
            Level level = entity.level();

            Vec3 start = new Vec3(from.getX() + 0.5, from.getY() + entity.getBbHeight() * 0.8, from.getZ() + 0.5);
            Vec3 end = new Vec3(to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5);
            Vec3 direction = end.subtract(start).normalize();
            double distance = start.distanceTo(end);
            double step = 0.5;

            for (double t = step; t <= distance; t += step) {
                Vec3 point = start.add(direction.scale(t));
                BlockPos checkPos = BlockPos.containing(point);

                if (checkPos.equals(from)) continue;
                if (checkPos.equals(to)) break;

                BlockState state = level.getBlockState(checkPos);

                if (isBreakableBlock(state, level, checkPos) && !isPassableBlock(state, level, checkPos)) {
                    return checkPos;
                }
            }
            return null;
        }

        private boolean isBreakableBlock(BlockState state, Level level, BlockPos pos) {
            return state.is(net.minecraft.world.level.block.Blocks.GLASS) ||
                    state.is(net.minecraft.world.level.block.Blocks.GLASS_PANE) ||
                    state.is(net.minecraft.world.level.block.Blocks.OAK_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.BIRCH_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.JUNGLE_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.ACACIA_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.MANGROVE_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.CHERRY_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.BAMBOO_DOOR) ||
                    state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE) ||
                    state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE) ||
                    state.getDestroySpeed(level, pos) < 5.0F;
        }

        private boolean isPassableBlock(BlockState state, Level level, BlockPos pos) {
            return state.isAir() ||
                    state.is(net.minecraft.world.level.block.Blocks.CAVE_AIR) ||
                    state.is(net.minecraft.world.level.block.Blocks.VOID_AIR) ||
                    state.liquid() ||
                    state.getCollisionShape(level, pos).isEmpty();
        }
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

    // ==================== 攻击方法（触发动画） ====================

    /**
     * 近战攻击 - 触发 attack 动画
     */
    public void performMeleeAttack() {
        if (attackCooldown > 0 || this.level().isClientSide) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = this.distanceTo(target);
        if (distance > 2.0) return;

        // 触发近战动画
        triggerMeleeAttackAnimation();

        attackCooldown = ATTACK_COOLDOWN_TICKS;

        // 造成伤害
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean hurt = target.hurt(this.damageSources().mobAttack(this), damage);

        if (hurt && !this.level().isClientSide) {
            // 音效和粒子
            this.playSound(ModSounds.GROUND_CHI.get(), 1.2F, 0.7F + this.random.nextFloat() * 0.4F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        2, 0.2, 0.2, 0.2, 0);
            }

            // 缠绕效果
            if (this.random.nextFloat() < 0.3f) {
                applyVineSnare(target);
            }

            handleKill(target);
        }
    }

    /**
     * 舌头攻击（中距离）- 触发 eat 动画（有舌头特效）
     */
    public void performTongueAttack() {
        if (tongueAttackCooldown > 0 || this.level().isClientSide) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = this.distanceTo(target);
        if (distance > TONGUE_RANGE || distance <= 2.0) return;

        // ========== 修复：添加视线检查，防止穿墙 ==========
        if (!hasLineOfSight(target)) {
            return;  // 被方块阻挡，不能攻击
        }
        // ================================================

        // 触发舌头攻击动画
        triggerTongueAttackAnimation();

        tongueAttackCooldown = TONGUE_COOLDOWN_TICKS;
        tongueTarget = target;

        // 造成伤害
        float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5);
        boolean hurt = target.hurt(this.damageSources().mobAttack(this), damage);

        if (hurt && !this.level().isClientSide) {
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
     * 破门/破窗攻击 - 使用 attackx 动画
     */
    public void performBreakAttack(BlockPos targetPos) {
        if (breakAttackCooldown > 0 || this.level().isClientSide) return;

        // 触发破窗动画
        triggerBreakAttackAnimation();
        breakAttackCooldown = BREAK_COOLDOWN_TICKS;

        // 破坏方块逻辑
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockState state = serverLevel.getBlockState(targetPos);
            if (isBreakableBlock(state, targetPos)) {
                serverLevel.destroyBlock(targetPos, true);
                this.playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.8F);

                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    /**
     * 合体/进化动画 - 触发 hypocrisy 动画
     */
    public void performFusionAnimation() {
        if (this.level().isClientSide) return;

        triggerHypocrisyAnimation();

        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        1, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    // ==================== 动画触发方法（修复 triggerAnim 调用） ====================

    private void triggerMeleeAttackAnimation() {
        if (this.level().isClientSide) return;
        try {
            // 正确用法：两个参数 (controllerName, animName)
            triggerAnim("meleeController", ANIM_MELEE_ATTACK);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("触发近战动画失败: {}", e.getMessage());
        }
    }

    private void triggerTongueAttackAnimation() {
        if (this.level().isClientSide) return;
        try {
            triggerAnim("tongueController", ANIM_TONGUE_ATTACK);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("触发舌头动画失败: {}", e.getMessage());
        }
    }

    private void triggerBreakAttackAnimation() {
        if (this.level().isClientSide) return;
        try {
            triggerAnim("breakController", ANIM_BREAK_ATTACK);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("触发破窗动画失败: {}", e.getMessage());
        }
    }

    private void triggerHypocrisyAnimation() {
        if (this.level().isClientSide) return;

        // 使用标志位方式，而不是 triggerAnim
        this.entityData.set(DATA_PLAYING_HYPOCRISY, true);
        this.hypocrisyAnimationTicks = HYPOCRISY_ANIMATION_DURATION;

        CorpseOrigin.LOGGER.debug("触发合体动画，标志位设置为 true");
    }

    private void triggerFusionCompleteAnimation() {
        triggerHypocrisyAnimation();
    }

    private void tickHypocrisyAnimation() {
        if (this.level().isClientSide) return;

        if (this.entityData.get(DATA_PLAYING_HYPOCRISY)) {
            this.hypocrisyAnimationTicks--;
            if (this.hypocrisyAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_HYPOCRISY, false);
                CorpseOrigin.LOGGER.debug("合体动画结束，清除标志位");
            }
        }
    }

    /**
     * 藤蔓缠绕（控制技能）
     */
    private void applyVineSnare(LivingEntity target) {
        if (target == null) return;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.02);
        }
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
        performFusionAnimation();

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

        // 使用舌头攻击动画来表现吞噬
        triggerTongueAttackAnimation();
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
            triggerTongueAttackAnimation();
        }

        if (!this.level().isClientSide) {
            stack.shrink(1);
        }

        return true;
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

    // ==================== 动画控制器注册 ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主动画控制器（循环动画：idle/walk）- 现在也处理合体动画
        AnimationController<CocoZombieXEntity> mainController = new AnimationController<>(this, "mainController", 5, this::mainAnimationController);

        // 近战攻击动画控制器（可触发）
        AnimationController<CocoZombieXEntity> meleeController = new AnimationController<>(this, "meleeController", 0, state -> PlayState.CONTINUE);
        meleeController.triggerableAnim(ANIM_MELEE_ATTACK, MELEE_ATTACK_ANIM);

        // 舌头攻击动画控制器（可触发）
        AnimationController<CocoZombieXEntity> tongueController = new AnimationController<>(this, "tongueController", 0, state -> PlayState.CONTINUE);
        tongueController.triggerableAnim(ANIM_TONGUE_ATTACK, TONGUE_ATTACK_ANIM);

        // 破窗攻击动画控制器（可触发）
        AnimationController<CocoZombieXEntity> breakController = new AnimationController<>(this, "breakController", 0, state -> PlayState.CONTINUE);
        breakController.triggerableAnim(ANIM_BREAK_ATTACK, BREAK_ATTACK_ANIM);

        controllers.add(mainController);
        controllers.add(meleeController);
        controllers.add(tongueController);
        controllers.add(breakController);
        // 删除 fusionController
    }

    /**
     * 主动画控制器 - 处理待机、行走、游泳（循环动画）
     */
    private <E extends CocoZombieXEntity> PlayState mainAnimationController(AnimationState<E> event) {
        // 优先播放合体动画（使用标志位）
        if (this.entityData.get(DATA_PLAYING_HYPOCRISY)) {
            return event.setAndContinue(HYPOCRISY_ANIM);
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

    // ==================== 辅助方法 ====================
// 修复 isBreakableBlock 方法 - 移除错误的类型转换
    private boolean isBreakableBlock(BlockState state, BlockPos pos) {
        return state.is(net.minecraft.world.level.block.Blocks.GLASS) ||
                state.is(net.minecraft.world.level.block.Blocks.GLASS_PANE) ||
                state.is(net.minecraft.world.level.block.Blocks.OAK_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.BIRCH_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.JUNGLE_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.ACACIA_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.MANGROVE_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.CHERRY_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.BAMBOO_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE) ||
                state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE) ||
                state.getDestroySpeed(this.level(), pos) < 5.0F;
    }

    // 添加一个只接收 BlockState 的重载方法（用于 BreakObstacleGoal 内部调用）
    private static boolean isBreakableBlock(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.GLASS) ||
                state.is(net.minecraft.world.level.block.Blocks.GLASS_PANE) ||
                state.is(net.minecraft.world.level.block.Blocks.OAK_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.BIRCH_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.JUNGLE_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.ACACIA_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.MANGROVE_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.CHERRY_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.BAMBOO_DOOR) ||
                state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE) ||
                state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE);
        // 注意：静态方法无法获取 Level 实例，所以不能使用 getDestroySpeed
    }

    // ==================== 导航切换 ====================
    private void updateNavigation() {
        if (this.isUnderWater()) {  // 只有完全在水下才用水路导航
            if (this.navigation != waterNavigation) {
                this.navigation = waterNavigation;
                this.navigation.stop();
            }
        } else {
            if (this.navigation != groundNavigation) {
                this.navigation = groundNavigation;
                this.navigation.stop();
            }
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
        // 完全参照一阶段企鹅尸兄
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
        // 关键：在 super.tick() 之前更新导航，确保 AI 决策时使用正确的导航器
        updateNavigation();

        // 强制让 AI 重新评估移动目标
        if (this.getTarget() != null && !this.getNavigation().isInProgress()) {
            this.getNavigation().moveTo(this.getTarget(), 1.3D);
        }

        super.tick();

        tickHypocrisyAnimation();

        if (attackCooldown > 0) attackCooldown--;
        if (tongueAttackCooldown > 0) tongueAttackCooldown--;
        if (breakAttackCooldown > 0) breakAttackCooldown--;

        // 合体动画计时
        if (isFusing) {
            fusionAnimationTimer--;
            if (fusionAnimationTimer <= 0) {
                isFusing = false;
                hasFused = true;
            }
        }

        if (!this.level().isClientSide) {
            hungerSystem.tick();
            handleRegularHungerDrain();
            handleAirSupply();
            handleSurfaceForAir();

            if (this.tickCount % 40 == 0) {
                tryEatNearbyCorpseGib();
            }

            tickFollowMaster();
            updateVisionRange();

            // 攻击逻辑
            if (this.getTarget() != null && this.getTarget().isAlive() && !isFusing) {
                double distance = this.distanceTo(this.getTarget());
                if (distance > 2.0 && distance <= TONGUE_RANGE && tongueAttackCooldown <= 0) {
                    performTongueAttack();
                } else if (distance <= 2.0 && attackCooldown <= 0) {
                    performMeleeAttack();
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
        triggerHypocrisyAnimation();

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
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.PAPER), 0.0f);
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

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY);
    }

    public int getMaxAir() {
        return MAX_AIR;
    }
}