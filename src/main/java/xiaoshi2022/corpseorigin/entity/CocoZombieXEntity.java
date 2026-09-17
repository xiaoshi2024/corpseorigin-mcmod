package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModSounds;

import java.util.EnumSet;
import java.util.UUID;

/**
 * CoCo 尸兄二阶段 - 企鹅与大叔的合体尸兄。
 * <p>
 * 模型自带完整合体造型，拥有近战 / 长舌 / 破窗三种攻击与多段合体动画；
 * 由一阶段企鹅尸兄击杀大叔后合体生成（见 {@code CocoZombieEntity#tryFuseWithUncle}）。
 */
public class CocoZombieXEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    // ==================== 动画 ====================
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    /** 模型没有独立的 swim 动画，用 walk 代替 */
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("walk");

    private static final RawAnimation MELEE_ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation TONGUE_ATTACK_ANIM = RawAnimation.begin().thenPlay("eat");
    private static final RawAnimation BREAK_ATTACK_ANIM = RawAnimation.begin().thenPlay("attackx");
    private static final RawAnimation HYPOCRISY_ANIM = RawAnimation.begin().thenPlay("hypocrisy");
    private static final RawAnimation CHIMERA_ANIM = RawAnimation.begin().thenPlay("chimera");

    private static final String ANIM_MELEE_ATTACK = "melee_attack";
    private static final String ANIM_TONGUE_ATTACK = "tongue_attack";
    private static final String ANIM_BREAK_ATTACK = "break_attack";

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TENTACLE_COUNT =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUSION_TIER =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_HYPOCRISY =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_CHIMERA =
            SynchedEntityData.defineId(CocoZombieXEntity.class, EntityDataSerializers.BOOLEAN);

    // ==================== 字段 ====================
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int kills = 0;
    private int attackCooldown = 0;
    private int tongueAttackCooldown = 0;
    private int breakAttackCooldown = 0;

    private static final int ATTACK_COOLDOWN_TICKS = 15;
    private static final int TONGUE_COOLDOWN_TICKS = 40;
    private static final int BREAK_COOLDOWN_TICKS = 30;
    private static final int TONGUE_RANGE = 12;

    private static final int MAX_AIR = 400;
    private static final int OXYGEN_WARNING_LEVEL = 30;
    private int surfaceCooldown = 0;

    private static final double FOLLOW_RANGE = 20.0D;
    private static final double TELEPORT_RANGE = 40.0D;
    private int followCooldown = 0;

    private static final int MAX_REGULAR_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1000;
    private int hungerDrainTimer = 0;

    private static final int MAX_CORPSE_HUNGER = 3;
    private int corpseHunger = 0;

    private int evolutionLevel = 1;

    private static final int HURT_MEMORY_DURATION = 200;
    private int lastHurtTick = -1000;

    private UUID masterUUID;

    private static final int HYPOCRISY_ANIMATION_DURATION = 60;
    private int hypocrisyAnimationTicks = 0;

    private static final int CHIMERA_ANIMATION_DURATION = 200;
    private int chimeraAnimationTicks = 0;

    /** 被合体掉的大叔，仅作记录 */
    private UUID uncleUUID;

    /** 合体状态：合体动画播完前不参与攻击 */
    private boolean isFusing = false;
    private boolean hasFused = false;
    private int fusionAnimationTimer = 0;

    private final WaterBoundPathNavigation waterNavigation;
    private final GroundPathNavigation groundNavigation;

    // ==================== 构造函数 ====================

    public CocoZombieXEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);

        this.waterNavigation = new WaterBoundPathNavigation(this, level);
        this.groundNavigation = new GroundPathNavigation(this, level);
        this.navigation = this.groundNavigation;
    }

    public CocoZombieXEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player player) {
        this(entityType, level);
        if (player != null) {
            this.masterUUID = player.getUUID();
        }
    }

    /**
     * 由一阶段企鹅尸兄击杀大叔后合体生成。
     * <p>
     * 一阶段身上的成长数据（击杀数 / 进化等级 / 饱食度 / 主人）全部继承过来。
     */
    public CocoZombieXEntity(Level level, CocoZombieEntity cocoZombie, UncleEntity uncle) {
        this(ModEntities.COCO_ZOMBIE_X, level);

        this.snapTo(cocoZombie.getX(), cocoZombie.getY(), cocoZombie.getZ(),
                cocoZombie.getYRot(), cocoZombie.getXRot());
        this.kills = cocoZombie.getKills();
        this.setEvolutionLevel(cocoZombie.getEvolutionLevel());
        this.setCorpseHunger(cocoZombie.getCorpseHunger());
        this.setRegularHunger(cocoZombie.getRegularHunger());

        if (cocoZombie.hasMaster()) {
            this.setMaster(cocoZombie.getMasterUUID());
        }

        if (uncle != null) {
            this.uncleUUID = uncle.getUUID();
        }

        // 合体特效
        this.setFusionTier(1);
        this.entityData.set(DATA_TENTACLE_COUNT, 4);
        this.setHealth(this.getMaxHealth());

        this.playFusionCompleteAnimation();
        this.level().broadcastEntityEvent(this, (byte) 20);

        CorpseOrigin.LOGGER.info("企鹅尸兄与大叔合体完成！进化等级: {}, 触手数量: {}",
                this.getEvolutionLevel(), this.entityData.get(DATA_TENTACLE_COUNT));
    }

    /** 合体完成：播 chimera 长动画，期间锁住攻击 */
    private void playFusionCompleteAnimation() {
        this.isFusing = true;
        this.fusionAnimationTimer = CHIMERA_ANIMATION_DURATION;

        triggerChimeraAnimation();
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUNGER, 720);
        builder.define(DATA_AIR_SUPPLY, MAX_AIR);
        builder.define(DATA_TENTACLE_COUNT, 4);
        builder.define(DATA_FUSION_TIER, 1);
        builder.define(DATA_PLAYING_HYPOCRISY, false);
        builder.define(DATA_PLAYING_CHIMERA, false);
    }

    // ==================== AI ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreakObstacleGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class,
                10, true, false, (target, level) -> this.shouldAttackNonCorpsePlayer(target)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Villager>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Animal>(this, Animal.class,
                10, true, false, (target, level) ->
                        !(target instanceof CocoZombieXEntity) && !(target instanceof CocoPenguinEntity)));

        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<Player>(this, Player.class,
                10, true, false, (target, level) -> this.shouldAttackCorpsePlayer(target)));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<CocoZombieXEntity>(this, CocoZombieXEntity.class,
                10, true, false, (target, level) -> this.shouldAttackOtherCorpseEntity(target)));
    }

    /**
     * 破坏障碍物目标：盯上房屋里的目标时，先砸掉挡路的玻璃和门。
     */
    public static class BreakObstacleGoal extends Goal {
        private final CocoZombieXEntity entity;
        private BlockPos targetBlockPos;
        private int breakTimer = 0;
        private static final int BREAK_INTERVAL = 20;
        private static final int SEARCH_RADIUS = 15;
        private static final int MAX_BREAK_DISTANCE = 4;

        public BreakObstacleGoal(CocoZombieXEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = entity.getTarget();
            if (target == null || !target.isAlive()) return false;

            if (entity.distanceTo(target) > SEARCH_RADIUS) return false;

            targetBlockPos = findBlockedObstacle(entity.blockPosition(), target.blockPosition());
            return targetBlockPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = entity.getTarget();
            if (target == null || !target.isAlive()) return false;

            if (targetBlockPos != null
                    && !isBreakableBlock(entity.level().getBlockState(targetBlockPos), entity.level(), targetBlockPos)) {
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

            if (entity.blockPosition().distSqr(targetBlockPos) > MAX_BREAK_DISTANCE * MAX_BREAK_DISTANCE) {
                entity.getNavigation().moveTo(targetBlockPos.getX() + 0.5, targetBlockPos.getY(),
                        targetBlockPos.getZ() + 0.5, 1.3D);
                return;
            }

            entity.getNavigation().stop();
            entity.getLookControl().setLookAt(targetBlockPos.getX() + 0.5, targetBlockPos.getY() + 0.5,
                    targetBlockPos.getZ() + 0.5);

            breakTimer++;
            if (breakTimer >= BREAK_INTERVAL) {
                entity.performBreakAttack(targetBlockPos);
                breakTimer = 0;
                targetBlockPos = findBlockedObstacle(entity.blockPosition(), target.blockPosition());
            }
        }

        /** 沿视线步进，找到第一个挡路的可破坏方块 */
        private BlockPos findBlockedObstacle(BlockPos from, BlockPos to) {
            Level level = entity.level();

            Vec3 start = new Vec3(from.getX() + 0.5, from.getY() + entity.getBbHeight() * 0.8, from.getZ() + 0.5);
            Vec3 end = new Vec3(to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5);
            Vec3 direction = end.subtract(start).normalize();
            double distance = start.distanceTo(end);
            double step = 0.5;

            for (double t = step; t <= distance; t += step) {
                BlockPos checkPos = BlockPos.containing(start.add(direction.scale(t)));

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
            return CocoZombieXEntity.isBreakableBlock(state) || state.getDestroySpeed(level, pos) < 5.0F;
        }

        private boolean isPassableBlock(BlockState state, Level level, BlockPos pos) {
            return state.isAir() || state.liquid() || state.getCollisionShape(level, pos).isEmpty();
        }
    }

    private static boolean isBreakableBlock(BlockState state) {
        return state.is(Blocks.GLASS)
                || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.OAK_DOOR)
                || state.is(Blocks.IRON_DOOR)
                || state.is(Blocks.SPRUCE_DOOR)
                || state.is(Blocks.BIRCH_DOOR)
                || state.is(Blocks.JUNGLE_DOOR)
                || state.is(Blocks.ACACIA_DOOR)
                || state.is(Blocks.DARK_OAK_DOOR)
                || state.is(Blocks.MANGROVE_DOOR)
                || state.is(Blocks.CHERRY_DOOR)
                || state.is(Blocks.BAMBOO_DOOR)
                || state.is(Blocks.OAK_FENCE)
                || state.is(Blocks.OAK_FENCE_GATE);
    }

    // ==================== 攻击判定 ====================

    private boolean shouldAttackNonCorpsePlayer(LivingEntity entity) {
        if (!(entity instanceof Player)) return false;
        if (isMaster(entity)) return false;
        return ZombieKin.isNotZombieKin(entity);
    }

    private boolean shouldAttackCorpsePlayer(LivingEntity entity) {
        if (!(entity instanceof Player)) return false;
        if (isMaster(entity)) return false;
        if (!ZombieKin.isZombieKin(entity)) return false;
        return wasRecentlyHurt();
    }

    private boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        if (entity == this) return false;
        if (entity instanceof CocoPenguinEntity) return false;
        if (!ZombieKin.isZombieKin(entity)) return false;
        return wasRecentlyHurt();
    }

    private boolean wasRecentlyHurt() {
        return (this.tickCount - this.lastHurtTick) < HURT_MEMORY_DURATION;
    }

    private boolean isMaster(LivingEntity entity) {
        return this.masterUUID != null && entity.getUUID().equals(this.masterUUID);
    }

    // ==================== 攻击方式 ====================

    /** 近战：触发 attack 动画 */
    public void performMeleeAttack() {
        if (this.attackCooldown > 0 || this.level().isClientSide()) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceTo(target) > 2.0) return;

        triggerAnim("meleeController", ANIM_MELEE_ATTACK);
        this.attackCooldown = ATTACK_COOLDOWN_TICKS;

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target.hurtServer((ServerLevel) this.level(), this.damageSources().mobAttack(this), damage)) {
            this.playSound(ModSounds.GROUND_CHI, 1.2F, 0.7F + this.random.nextFloat() * 0.4F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        2, 0.2, 0.2, 0.2, 0);
            }

            if (this.random.nextFloat() < 0.3f) {
                applyVineSnare(target);
            }
            handleKill(target);
        }
    }

    /** 长舌攻击：中距离触发 eat 动画，带击退 */
    public void performTongueAttack() {
        if (this.tongueAttackCooldown > 0 || this.level().isClientSide()) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = this.distanceTo(target);
        if (distance > TONGUE_RANGE || distance <= 2.0) return;
        // 挡住视线就不能隔墙舔
        if (!this.hasLineOfSight(target)) return;

        triggerAnim("tongueController", ANIM_TONGUE_ATTACK);
        this.tongueAttackCooldown = TONGUE_COOLDOWN_TICKS;

        float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5);
        if (target.hurtServer((ServerLevel) this.level(), this.damageSources().mobAttack(this), damage)) {
            Vec3 knockback = target.position().subtract(this.position()).normalize().scale(1.2);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));

            this.playSound(SoundEvents.FOX_TELEPORT, 1.5F, 0.6F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    /** 破门/破窗：触发 attackx 动画 */
    public void performBreakAttack(BlockPos targetPos) {
        if (this.breakAttackCooldown > 0 || this.level().isClientSide()) return;

        triggerAnim("breakController", ANIM_BREAK_ATTACK);
        this.breakAttackCooldown = BREAK_COOLDOWN_TICKS;

        if (this.level() instanceof ServerLevel serverLevel
                && isBreakableBlock(serverLevel.getBlockState(targetPos))) {
            serverLevel.destroyBlock(targetPos, true, this, 512);
            this.playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.8F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0);
        }
    }

    /** 合体动作：触发 hypocrisy 动画 + 灵魂火特效 */
    public void performFusionAnimation() {
        if (this.level().isClientSide()) return;

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

    private void triggerHypocrisyAnimation() {
        if (this.level().isClientSide()) return;
        this.entityData.set(DATA_PLAYING_HYPOCRISY, true);
        this.hypocrisyAnimationTicks = HYPOCRISY_ANIMATION_DURATION;
        this.getNavigation().stop();
    }

    private void triggerChimeraAnimation() {
        if (this.level().isClientSide()) return;
        this.entityData.set(DATA_PLAYING_CHIMERA, true);
        this.chimeraAnimationTicks = CHIMERA_ANIMATION_DURATION;
        this.getNavigation().stop();
    }

    private void tickHypocrisyAnimation() {
        if (this.level().isClientSide()) return;

        if (this.entityData.get(DATA_PLAYING_HYPOCRISY)) {
            this.hypocrisyAnimationTicks--;
            if (this.hypocrisyAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_HYPOCRISY, false);
            }
        }
    }

    private void tickChimeraAnimation() {
        if (this.level().isClientSide()) return;

        if (this.entityData.get(DATA_PLAYING_CHIMERA)) {
            this.chimeraAnimationTicks--;
            if (this.chimeraAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_CHIMERA, false);
            }
        }
    }

    /** 藤蔓缠绕：减速 + 虚弱 */
    private void applyVineSnare(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void handleKill(Entity target) {
        this.kills++;

        if (target instanceof LivingEntity living) {
            this.heal(living.getMaxHealth() * 0.25F);
        }

        addCorpseHunger(2);
        addRegularHunger(30);

        checkEvolution();
    }

    // ==================== 进化 ====================

    private void checkEvolution() {
        if (this.kills >= this.evolutionLevel * 10 && this.evolutionLevel < 8) {
            evolve();
        }
    }

    private void evolve() {
        this.evolutionLevel++;
        this.kills = 0;

        double healthBonus = this.evolutionLevel * 6.0;
        double damageBonus = this.evolutionLevel * 1.5;
        double speedBonus = this.evolutionLevel * 0.04;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(65.0 + healthBonus);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(9.0 + damageBonus);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32 + speedBonus);
        this.getAttribute(Attributes.ARMOR).setBaseValue(6.0 + this.evolutionLevel);

        this.setHealth(this.getMaxHealth());

        this.entityData.set(DATA_TENTACLE_COUNT, Math.min(8, 4 + this.evolutionLevel / 2));

        // 进化时播真正的合体动画
        triggerChimeraAnimation();

        if (this.evolutionLevel >= 3) {
            setFusionTier(2);
        }
        if (this.evolutionLevel >= 6) {
            setFusionTier(3);
        }

        this.level().broadcastEntityEvent(this, (byte) 20);
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);

        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        1, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    public int getEvolutionLevel() {
        return this.evolutionLevel;
    }

    public void setEvolutionLevel(int level) {
        this.evolutionLevel = Math.max(1, level);
    }

    // ==================== 喂养 / 饱食度 ====================

    public boolean feedFish(ItemStack stack) {
        Item item = stack.getItem();
        int restoreAmount;
        if (item == Items.SALMON) restoreAmount = 40;
        else if (item == Items.COD) restoreAmount = 35;
        else if (item == Items.TROPICAL_FISH) restoreAmount = 25;
        else if (item == Items.PUFFERFISH) restoreAmount = 15;
        else return false;

        addRegularHunger(restoreAmount);

        if (!this.isInWater()) {
            triggerAnim("tongueController", ANIM_TONGUE_ATTACK);
        }
        if (!this.level().isClientSide()) {
            stack.shrink(1);
        }
        return true;
    }

    public void addRegularHunger(int amount) {
        this.entityData.set(DATA_HUNGER, Math.min(MAX_REGULAR_HUNGER, getRegularHunger() + amount));

        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(amount / 5.0F);
        }
    }

    public int getRegularHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setRegularHunger(int hunger) {
        this.entityData.set(DATA_HUNGER, Math.max(0, Math.min(MAX_REGULAR_HUNGER, hunger)));
    }

    private void addCorpseHunger(int amount) {
        this.corpseHunger = Math.max(0, Math.min(MAX_CORPSE_HUNGER, this.corpseHunger + amount));
    }

    public int getCorpseHunger() {
        return this.corpseHunger;
    }

    public void setCorpseHunger(int hunger) {
        this.corpseHunger = Math.max(0, Math.min(MAX_CORPSE_HUNGER, hunger));
    }

    // ==================== 氧气 ====================

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
        if (this.surfaceCooldown > 0) {
            this.surfaceCooldown--;
            return;
        }

        if (this.entityData.get(DATA_AIR_SUPPLY) <= OXYGEN_WARNING_LEVEL && this.isInWater() && this.isUnderWater()) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.4, this.getDeltaMovement().z);
            this.surfaceCooldown = 60;
        }
    }

    private void handleRegularHungerDrain() {
        if (this.level().isClientSide()) return;

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
        controllers.add(new AnimationController<>("mainController", 5, this::mainAnimationController));

        controllers.add(new AnimationController<>("meleeController", 0, state -> PlayState.CONTINUE)
                .triggerableAnim(ANIM_MELEE_ATTACK, MELEE_ATTACK_ANIM));
        controllers.add(new AnimationController<>("tongueController", 0, state -> PlayState.CONTINUE)
                .triggerableAnim(ANIM_TONGUE_ATTACK, TONGUE_ATTACK_ANIM));
        controllers.add(new AnimationController<>("breakController", 0, state -> PlayState.CONTINUE)
                .triggerableAnim(ANIM_BREAK_ATTACK, BREAK_ATTACK_ANIM));
    }

    private PlayState mainAnimationController(AnimationTest<CocoZombieXEntity> test) {
        if (this.entityData.get(DATA_PLAYING_CHIMERA)) {
            return test.setAndContinue(CHIMERA_ANIM);
        }
        if (this.entityData.get(DATA_PLAYING_HYPOCRISY)) {
            return test.setAndContinue(HYPOCRISY_ANIM);
        }
        if (this.isInWater()) {
            return test.setAndContinue(SWIM_ANIM);
        }
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 导航切换 ====================

    private void updateNavigation() {
        // 只有完全潜入水下才走水路
        if (this.isUnderWater()) {
            if (this.navigation != this.waterNavigation) {
                this.navigation = this.waterNavigation;
                this.navigation.stop();
            }
        } else if (this.navigation != this.groundNavigation) {
            this.navigation = this.groundNavigation;
            this.navigation.stop();
        }
    }

    // ==================== 主人跟随 ====================

    private void tickFollowMaster() {
        if (this.masterUUID == null) return;

        if (this.followCooldown > 0) {
            this.followCooldown--;
            return;
        }
        if (this.tickCount % 10 != 0) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Player master = serverLevel.getServer().getPlayerList().getPlayer(this.masterUUID);
        if (master == null || !master.isAlive()) return;

        double distanceToMaster = this.distanceToSqr(master);
        if (distanceToMaster > TELEPORT_RANGE * TELEPORT_RANGE) {
            teleportToMaster(master);
        } else if (distanceToMaster > FOLLOW_RANGE * FOLLOW_RANGE) {
            this.getNavigation().moveTo(master, 1.3D);
            this.followCooldown = 20;
        }
    }

    private void teleportToMaster(Player master) {
        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = 2 + this.random.nextDouble() * 2;
            double targetX = master.getX() + Math.cos(angle) * distance;
            double targetZ = master.getZ() + Math.sin(angle) * distance;
            double targetY = master.getY();

            if (this.level().noCollision(this.getBoundingBox()
                    .move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ()))) {
                this.teleportTo(targetX, targetY, targetZ);
                this.getNavigation().stop();
                this.followCooldown = 40;
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

    // ==================== 状态效果 / 受击 ====================

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value() == MobEffects.POISON.value()
                || effect.getEffect().value() == MobEffects.HUNGER.value()) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && !isMaster(attacker)) {
            this.lastHurtTick = this.tickCount;
        }
        return super.hurtServer(level, source, amount);
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        // 先切导航，AI 决策才会用对导航器
        this.updateNavigation();

        if (this.getTarget() != null && !this.getNavigation().isInProgress()) {
            this.getNavigation().moveTo(this.getTarget(), 1.3D);
        }

        super.tick();

        tickHypocrisyAnimation();
        tickChimeraAnimation();

        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.tongueAttackCooldown > 0) this.tongueAttackCooldown--;
        if (this.breakAttackCooldown > 0) this.breakAttackCooldown--;

        // 合体动画总计时，结束后才放开攻击
        if (this.isFusing) {
            this.fusionAnimationTimer--;
            if (this.fusionAnimationTimer <= 0) {
                this.isFusing = false;
                this.hasFused = true;
            }
        }

        if (!this.level().isClientSide()) {
            handleRegularHungerDrain();
            handleAirSupply();
            handleSurfaceForAir();
            tickFollowMaster();
            updateVisionRange();

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && !this.isFusing) {
                double distance = this.distanceTo(target);
                if (distance > 2.0 && distance <= TONGUE_RANGE && this.tongueAttackCooldown <= 0) {
                    performTongueAttack();
                } else if (distance <= 2.0 && this.attackCooldown <= 0) {
                    performMeleeAttack();
                }
            }
        }
    }

    private void updateVisionRange() {
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(20.0D + this.evolutionLevel * 2.5D);
        }
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Kills", this.kills);
        output.putInt("RegularHunger", getRegularHunger());
        output.putInt("AirSupply", this.entityData.get(DATA_AIR_SUPPLY));
        output.putInt("CorpseHunger", this.corpseHunger);
        output.putInt("EvolutionLevel", this.evolutionLevel);
        output.putInt("FusionTier", getFusionTier());
        output.putInt("TentacleCount", this.entityData.get(DATA_TENTACLE_COUNT));
        output.putBoolean("HasFused", this.hasFused);
        if (this.masterUUID != null) {
            output.putString("MasterUUID", this.masterUUID.toString());
        }
        if (this.uncleUUID != null) {
            output.putString("UncleUUID", this.uncleUUID.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.kills = input.getIntOr("Kills", this.kills);
        this.entityData.set(DATA_HUNGER, input.getIntOr("RegularHunger", getRegularHunger()));
        this.entityData.set(DATA_AIR_SUPPLY, input.getIntOr("AirSupply", MAX_AIR));
        this.corpseHunger = input.getIntOr("CorpseHunger", this.corpseHunger);
        this.evolutionLevel = Math.max(1, input.getIntOr("EvolutionLevel", this.evolutionLevel));
        setFusionTier(input.getIntOr("FusionTier", getFusionTier()));
        this.entityData.set(DATA_TENTACLE_COUNT, input.getIntOr("TentacleCount",
                this.entityData.get(DATA_TENTACLE_COUNT)));
        this.hasFused = input.getBooleanOr("HasFused", this.hasFused);
        input.getString("MasterUUID").ifPresent(uuid -> this.masterUUID = UUID.fromString(uuid));
        input.getString("UncleUUID").ifPresent(uuid -> this.uncleUUID = UUID.fromString(uuid));
    }

    // ==================== 合体阶段 ====================

    public int getFusionTier() {
        return this.entityData.get(DATA_FUSION_TIER);
    }

    public void setFusionTier(int tier) {
        this.entityData.set(DATA_FUSION_TIER, Math.max(1, Math.min(3, tier)));
    }

    public int getTentacleCount() {
        return this.entityData.get(DATA_TENTACLE_COUNT);
    }

    public UUID getUncleUUID() {
        return this.uncleUUID;
    }

    public boolean isFusing() {
        return this.isFusing;
    }

    public boolean hasFused() {
        return this.hasFused;
    }

    // ==================== 主人系统 ====================

    public void setMaster(UUID masterUUID) {
        this.masterUUID = masterUUID;
    }

    public UUID getMasterUUID() {
        return this.masterUUID;
    }

    public boolean hasMaster() {
        return this.masterUUID != null;
    }

    // ==================== 交互 ====================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (feedFish(player.getItemInHand(hand))) {
            return InteractionResult.SUCCESS;
        }

        if (isMaster(player)) {
            if (!this.level().isClientSide()) {
                player.sendOverlayMessage(Component.literal(
                        "§c§l合体尸兄(CoCo+大叔)§r§e - 等级: " + getEvolutionLevel()
                                + " | 合体阶段: " + getFusionTier()
                                + " | 触手: " + getTentacleCount()
                                + " | 击杀: " + this.kills
                                + " | 尸兄饱腹: " + getCorpseHunger()
                                + " | 饥饿: " + getRegularHunger()
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

        if (this.random.nextFloat() < 0.5f) {
            this.spawnAtLocation(level, new ItemStack(Items.SALMON), 0.0f);
        }
    }

    // ==================== Getter ====================

    public int getKills() {
        return this.kills;
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
