package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede;

import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedHead;
import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedJoint;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.register.EntityRegistry;
import com.phagens.corpseorigin.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CentipedeHead extends AbstractSegmentedHead implements GeoEntity, ICorpseBrother, ICorpseHunger {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<CentipedeJoint> segments = new ArrayList<>();
    private boolean segmentsInitialized = false;

    // 动画定义
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("gtwo");
    protected static final RawAnimation MOUTH_OPEN_ANIM = RawAnimation.begin().thenPlay("gone").thenLoop("gone");

    // 同步数据
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_ATTACK =
            SynchedEntityData.defineId(CentipedeHead.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICKS =
            SynchedEntityData.defineId(CentipedeHead.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_COOLDOWN =
            SynchedEntityData.defineId(CentipedeHead.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_MOUTH_OPEN =
            SynchedEntityData.defineId(CentipedeHead.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_MOUTH_OPEN_TICKS =
            SynchedEntityData.defineId(CentipedeHead.class, EntityDataSerializers.INT);

    private static final int ATTACK_ANIMATION_DURATION = 45;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int MOUTH_OPEN_DURATION = 20;

    // 遁地相关
    public boolean isUnderground = false;
    public boolean isBurrowed = false;
    private int undergroundTimer = 0;
    private int burrowedIdleTimer = 0;
    private static final int UNDERGROUND_DURATION = 100;
    private static final int BURROWED_IDLE_DURATION = 60;
    private static final int UNDERGROUND_COOLDOWN = 200;
    private static final int UNDERGROUND_CHECK_INTERVAL = 10;
    private int undergroundCheckTimer = 0;

    // 洞穴偏好
    private BlockPos preferredCavePos = null;
    private int caveSearchCooldown = 0;

    // 攻击力
    private static final float BASE_ATTACK_DAMAGE = 6.0f;
    private static final float ATTACK_BONUS_PER_SEGMENT = 1.0f;

    // 弯曲效果参数
    private static final float MAX_SEGMENT_ANGLE = 35.0f;
    private static final float ANGLE_SMOOTHING = 0.4f;
    private static final float TURN_RESPONSE_FACTOR = 0.6f;
    private static final float MIN_TURN_SPEED = 0.05f;

    // 历史记录
    private final List<Vec3> headPositionHistory = new ArrayList<>();
    private final List<Float> headYawHistory = new ArrayList<>();
    private static final int HISTORY_SIZE = 20;

    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);
    private int eatAnimationTimer = 0;

    public CentipedeHead(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.baseHealthPerSegment = 30.0f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYING_ATTACK, false);
        builder.define(DATA_ATTACK_TICKS, 0);
        builder.define(DATA_ATTACK_COOLDOWN, 0);
        builder.define(DATA_MOUTH_OPEN, false);
        builder.define(DATA_MOUTH_OPEN_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CentipedeMeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateHeadHistory();

            float customMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
            float customCurrentHealth = this.entityData.get(DATA_TOTAL_HEALTH);

            if (Math.abs(this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() - customMaxHealth) > 0.1f) {
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(customMaxHealth);
            }
            if (Math.abs(this.getHealth() - customCurrentHealth) > 0.1f) {
                this.setHealth(customCurrentHealth);
            }
            updateAttackDamageBasedOnHealth();

            if (!segmentsInitialized) {
                if (!segmentUUIDs.isEmpty()) {
                    deleteAllOldSegments();
                }
                int expectedCount = calculateExpectedSegmentCount();
                if (segments.isEmpty()) {
                    initializeSegments(expectedCount);
                }
                segmentsInitialized = true;
                logSegmentInfo("初始化完成");
            }

            segments.removeIf(segment -> !segment.isAlive());
            ensureChainIntegrity();
            applyNaturalBending();

            if (!segments.isEmpty()) {
                syncSegmentUUIDs();
            }
        }

        tickAttackAnimation();
        tickMouthAnimation();
        tickEatAnimation();

        if (!level().isClientSide) {
            tickUnderground();
            tickSurfaceBehavior();
        }
    }

    // ==================== 动画相关方法 ====================

    private void tickMouthAnimation() {
        if (this.entityData.get(DATA_MOUTH_OPEN)) {
            int mouthTicks = this.entityData.get(DATA_MOUTH_OPEN_TICKS);
            mouthTicks--;
            this.entityData.set(DATA_MOUTH_OPEN_TICKS, mouthTicks);

            if (mouthTicks <= 0) {
                this.entityData.set(DATA_MOUTH_OPEN, false);
            }
        }
    }

    private void tickEatAnimation() {
        if (eatAnimationTimer > 0) {
            eatAnimationTimer--;
            if (eatAnimationTimer == 0) {
                closeMouth();
            }
        }
    }

    private void tickAttackAnimation() {
        if (this.entityData.get(DATA_PLAYING_ATTACK)) {
            int attackTicks = this.entityData.get(DATA_ATTACK_TICKS);
            attackTicks--;
            this.entityData.set(DATA_ATTACK_TICKS, attackTicks);

            if (attackTicks <= 0) {
                this.entityData.set(DATA_PLAYING_ATTACK, false);
            }
        }

        int attackCooldown = this.entityData.get(DATA_ATTACK_COOLDOWN);
        if (attackCooldown > 0) {
            this.entityData.set(DATA_ATTACK_COOLDOWN, attackCooldown - 1);
        }
    }

    public void openMouth() {
        if (level().isClientSide) return;
        this.entityData.set(DATA_MOUTH_OPEN, true);
        this.entityData.set(DATA_MOUTH_OPEN_TICKS, MOUTH_OPEN_DURATION);
    }

    public void closeMouth() {
        if (level().isClientSide) return;
        this.entityData.set(DATA_MOUTH_OPEN, false);
        this.entityData.set(DATA_MOUTH_OPEN_TICKS, 0);
    }

    public void triggerEatAnimation() {
        if (level().isClientSide) return;
        openMouth();
        eatAnimationTimer = MOUTH_OPEN_DURATION;
        this.playSound(ModSounds.GROUND_CHI.get(), 0.8F, 1.0F + random.nextFloat() * 0.3F);
    }

    // ==================== 遁地系统 ====================

    private void tickSurfaceBehavior() {
        if (isUnderground || isBurrowed) return;

        if (getTarget() == null && this.getNavigation().isDone()) {
            if (caveSearchCooldown <= 0) {
                BlockPos cavePos = findNearestCaveEntrance();
                if (cavePos != null && !isAboveCave(cavePos)) {
                    this.getNavigation().moveTo(cavePos.getX() + 0.5, cavePos.getY(), cavePos.getZ() + 0.5, 0.8D);
                    caveSearchCooldown = 40;
                }
                caveSearchCooldown--;
            } else {
                caveSearchCooldown--;
            }
        }
    }

    private BlockPos findNearestCaveEntrance() {
        BlockPos nearestCave = null;
        double nearestDistance = 64.0D;

        BlockPos centerPos = this.blockPosition();
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -5; dy <= 5; dy++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    BlockState aboveState = level().getBlockState(checkPos.above());
                    BlockState state = level().getBlockState(checkPos);
                    BlockState belowState = level().getBlockState(checkPos.below());

                    if (state.isAir() && belowState.isSolid() && !aboveState.isSolid()) {
                        if (hasCaveSpaceBelow(checkPos.below())) {
                            double distance = this.distanceToSqr(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestCave = checkPos;
                            }
                        }
                    }
                }
            }
        }
        return nearestCave;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        // 遁地或钻入状态时免疫摔落伤害
        if (isUnderground || isBurrowed) {
            return false;
        }

        // 检查是否在洞穴中（周围有空气洞穴）
        if (isInCave()) {
            return false;
        }

        // 检查下方是否有洞穴空间（即将落入洞穴）
        if (hasCaveSpaceBelow(this.blockPosition())) {
            return false;
        }

        return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    /**
     * 检查是否在洞穴中
     */
    private boolean isInCave() {
        // 检查周围是否有足够的洞穴空间
        int caveBlocks = 0;
        BlockPos centerPos = this.blockPosition();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);
                    if (state.isAir()) {
                        caveBlocks++;
                        if (caveBlocks >= 10) { // 足够多的空气方块表示在洞穴中
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isNoGravity() {
        // 遁地时无重力
        if (isUnderground || isBurrowed) {
            return true;
        }
        return super.isNoGravity();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // 遁地时免疫摔落和墙壁伤害
        if ((isUnderground || isBurrowed) &&
                (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING))) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    private boolean hasCaveSpaceBelow(BlockPos pos) {
        int caveSpace = 0;
        for (int dy = 1; dy <= 5; dy++) {
            BlockPos checkPos = pos.below(dy);
            if (level().getBlockState(checkPos).isAir()) {
                caveSpace++;
            } else if (level().getBlockState(checkPos).isSolid()) {
                break;
            }
        }
        return caveSpace >= 2;
    }

    private boolean isAboveCave(BlockPos pos) {
        for (int dy = 1; dy <= 5; dy++) {
            BlockPos checkPos = pos.below(dy);
            if (level().getBlockState(checkPos).isAir()) {
                return true;
            }
        }
        return false;
    }

    private void tickUnderground() {
        undergroundCheckTimer++;

        if (isUnderground) {
            undergroundTimer--;
            noPhysics = true;

            if (level() instanceof ServerLevel serverLevel && undergroundCheckTimer % 3 == 0) {
                spawnBurrowParticles(serverLevel);
            }

            if (undergroundCheckTimer % 10 == 0) {
                this.playSound(SoundEvents.GRASS_BREAK, 0.6F, 0.8F + random.nextFloat() * 0.4F);
            }

            if (getTarget() != null && getTarget().isAlive()) {
                moveTowardsCaveOrTarget();
            } else {
                moveDeeperIntoCave();
            }

            if (undergroundTimer <= 0) {
                prepareToSurface();
            }

        } else if (isBurrowed) {
            burrowedIdleTimer--;
            noPhysics = true;

            if (level() instanceof ServerLevel serverLevel && tickCount % 20 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        getX(), getY() + 0.2, getZ(),
                        1, 0.1, 0.1, 0.1, 0.01);
            }

            if (burrowedIdleTimer <= 0 || (getTarget() != null && getTarget().isAlive())) {
                exitUnderground();
            }

        } else {
            noPhysics = false;

            if (undergroundCheckTimer >= UNDERGROUND_CHECK_INTERVAL) {
                undergroundCheckTimer = 0;
                if (canEnterUnderground()) {
                    enterUnderground();
                }
            }
        }
    }

    private void spawnBurrowParticles(ServerLevel serverLevel) {
        double offsetX = (random.nextDouble() - 0.5) * 1.5;
        double offsetY = (random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (random.nextDouble() - 0.5) * 1.5;

        serverLevel.sendParticles(ParticleTypes.CLOUD,
                getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                1, 0.1, 0.1, 0.1, 0.03);

        if (random.nextInt(5) == 0) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    getX() + offsetX, getY() + 0.5, getZ() + offsetZ,
                    1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void moveDeeperIntoCave() {
        BlockPos deeperCave = findDeeperCavePosition();

        if (deeperCave != null) {
            double dx = deeperCave.getX() + 0.5 - this.getX();
            double dy = deeperCave.getY() + 0.5 - this.getY();
            double dz = deeperCave.getZ() + 0.5 - this.getZ();

            this.setDeltaMovement(
                    this.getDeltaMovement().x + dx * 0.02,
                    this.getDeltaMovement().y + dy * 0.01,
                    this.getDeltaMovement().z + dz * 0.02
            );
        } else {
            this.setDeltaMovement(
                    this.getDeltaMovement().x,
                    Math.max(-0.1, this.getDeltaMovement().y - 0.005),
                    this.getDeltaMovement().z
            );
        }
    }

    private BlockPos findDeeperCavePosition() {
        BlockPos bestPos = null;
        int lowestY = this.getBlockY();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy >= -5; dy--) {
                    BlockPos checkPos = this.blockPosition().offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);

                    if (state.isAir() && checkPos.getY() < lowestY) {
                        if (hasSpaceAtPosition(checkPos)) {
                            lowestY = checkPos.getY();
                            bestPos = checkPos;
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean hasSpaceAtPosition(BlockPos pos) {
        int space = 0;
        for (int dy = 0; dy <= 2; dy++) {
            if (level().getBlockState(pos.above(dy)).isAir()) {
                space++;
            } else {
                break;
            }
        }
        return space >= 2;
    }

    private void moveTowardsCaveOrTarget() {
        if (getTarget() == null) return;

        BlockPos cavePos = findNearbyCave();

        if (cavePos != null) {
            double dx = cavePos.getX() + 0.5 - this.getX();
            double dz = cavePos.getZ() + 0.5 - this.getZ();

            this.setDeltaMovement(
                    this.getDeltaMovement().x + dx * 0.025,
                    this.getDeltaMovement().y,
                    this.getDeltaMovement().z + dz * 0.025
            );

            if (this.distanceToSqr(cavePos.getX(), cavePos.getY(), cavePos.getZ()) < 9) {
                moveBehindTarget();
            }
        } else {
            moveBehindTarget();
        }
    }

    private void moveBehindTarget() {
        LivingEntity target = getTarget();
        if (target == null) return;

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();

        double perpX = -dz;
        double perpZ = dx;
        double length = Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (length > 0) {
            perpX /= length;
            perpZ /= length;
        }

        this.setDeltaMovement(
                this.getDeltaMovement().x + perpX * 0.03,
                this.getDeltaMovement().y,
                this.getDeltaMovement().z + perpZ * 0.03
        );
    }

    private BlockPos findNearbyCave() {
        BlockPos nearestCave = null;
        double nearestDistance = 25.0D;

        BlockPos centerPos = this.blockPosition();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -3; dy <= 1; dy++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);

                    if (state.isAir() && hasCaveSpaceBelow(checkPos)) {
                        double distance = this.distanceToSqr(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestCave = checkPos;
                        }
                    }
                }
            }
        }
        return nearestCave;
    }

    private BlockPos findNearestCave() {
        BlockPos nearestCave = null;
        double nearestDistance = Double.MAX_VALUE;

        BlockPos centerPos = this.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 0; dy++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);

                    if (state.isAir()) {
                        double distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestCave = checkPos;
                        }
                    }
                }
            }
        }
        return nearestCave;
    }

    private boolean canEnterUnderground() {
        if (undergroundTimer > 0) return false;
        if (this.entityData.get(DATA_ATTACK_COOLDOWN) > 0) return false;
        if (!this.onGround()) return false;

        boolean hasCave = hasCaveBelow() || hasCaveNearby();

        if (getTarget() != null && getTarget().isAlive()) {
            double distance = this.distanceTo(getTarget());

            if (distance <= 15 && distance >= 3) {
                if (hasCave) {
                    return true;
                }
                BlockPos feetPos = this.blockPosition().below();
                BlockState feetState = level().getBlockState(feetPos);
                if (!feetState.is(Blocks.BEDROCK) && feetState.isSolid()) {
                    return true;
                }
            }
            return false;
        }

        if (hasCave) {
            BlockPos cavePos = findNearestCaveEntrance();
            if (cavePos != null && this.distanceToSqr(cavePos.getX(), cavePos.getY(), cavePos.getZ()) < 16) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCaveBelow() {
        for (int dy = 1; dy <= 5; dy++) {
            BlockPos checkPos = this.blockPosition().below(dy);
            BlockState state = level().getBlockState(checkPos);
            if (state.isAir()) {
                if (hasCaveSpaceBelow(checkPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCaveNearby() {
        BlockPos centerPos = this.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= -1; dy++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);
                    if (state.isAir() && hasCaveSpaceBelow(checkPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void enterUnderground() {
        isUnderground = true;
        isBurrowed = false;
        undergroundTimer = UNDERGROUND_DURATION;

        BlockPos cavePos = findNearestCave();
        if (cavePos != null) {
            preferredCavePos = cavePos;
        }

        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.6F);
        this.playSound(SoundEvents.SAND_BREAK, 1.0F, 0.8F);

        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 2;
                double offsetY = (random.nextDouble() - 0.5) * 2 - 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 2;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        getX() + offsetX, getY() + 0.5 + offsetY, getZ() + offsetZ,
                        1, 0.1, 0.1, 0.1, 0.05);
                serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    private void prepareToSurface() {
        isUnderground = false;
        isBurrowed = true;
        burrowedIdleTimer = BURROWED_IDLE_DURATION;

        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 5; i++) {
                serverLevel.sendParticles(ParticleTypes.MYCELIUM,
                        getX(), getY() + 0.2, getZ(),
                        1, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    private void exitUnderground() {
        isUnderground = false;
        isBurrowed = false;
        undergroundTimer = UNDERGROUND_COOLDOWN;
        preferredCavePos = null;

        teleportToGround();

        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
        this.playSound(SoundEvents.SAND_PLACE, 1.0F, 0.7F);

        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 2.5;
                double offsetY = random.nextDouble() * 2;
                double offsetZ = (random.nextDouble() - 0.5) * 2.5;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        getX() + offsetX, getY() + 0.5, getZ() + offsetZ,
                        1, 0.2, 0.2, 0.2, 0);
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        getX() + offsetX, getY() + 0.5 + offsetY, getZ() + offsetZ,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        if (getTarget() != null && getTarget().isAlive() && this.distanceTo(getTarget()) < 5) {
            triggerAttackAnimation();
        }
    }

    private void teleportToGround() {
        BlockPos validPos = findValidSurfacePosition(this.blockPosition());

        if (validPos == null) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    validPos = findValidSurfacePosition(this.blockPosition().offset(dx, 0, dz));
                    if (validPos != null) break;
                }
                if (validPos != null) break;
            }
        }

        if (validPos != null) {
            this.moveTo(validPos.getX() + 0.5, validPos.getY(), validPos.getZ() + 0.5, this.getYRot(), this.getXRot());
        }
    }

    private BlockPos findValidSurfacePosition(BlockPos startPos) {
        for (int y = startPos.getY(); y > level().getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockState state = level().getBlockState(checkPos);

            if (state.isSolid() && !state.is(Blocks.BEDROCK)) {
                BlockPos abovePos = checkPos.above();
                BlockState aboveState = level().getBlockState(abovePos);
                BlockState twoAboveState = level().getBlockState(abovePos.above());

                if (!aboveState.isSolid() && !twoAboveState.isSolid()) {
                    return abovePos;
                }
            }
        }
        return null;
    }

    // ==================== 身体弯曲系统 ====================

    private void updateHeadHistory() {
        Vec3 currentPos = this.position();
        float currentYaw = this.getYRot();

        headPositionHistory.add(currentPos);
        headYawHistory.add(currentYaw);

        while (headPositionHistory.size() > HISTORY_SIZE) {
            headPositionHistory.remove(0);
        }
        while (headYawHistory.size() > HISTORY_SIZE) {
            headYawHistory.remove(0);
        }
    }

    private void applyNaturalBending() {
        if (segments.size() < 2) return;

        float turnSpeed = calculateTurnSpeed();

        if (Math.abs(turnSpeed) > MIN_TURN_SPEED) {
            applyTurnBending(turnSpeed);
        }

        applySegmentAngleConstraints();
        applyTailSway();
    }

    private float calculateTurnSpeed() {
        if (headYawHistory.size() < 2) return 0;

        float prevYaw = headYawHistory.get(headYawHistory.size() - 2);
        float currentYaw = headYawHistory.get(headYawHistory.size() - 1);
        float deltaYaw = Mth.wrapDegrees(currentYaw - prevYaw);

        return deltaYaw * 20;
    }

    private void applyTurnBending(float turnSpeed) {
        for (int i = 1; i < segments.size(); i++) {
            CentipedeJoint segment = segments.get(i);
            CentipedeJoint prevSegment = segments.get(i - 1);

            float tailFactor = (float) i / segments.size();
            float extraAngle = turnSpeed * TURN_RESPONSE_FACTOR * tailFactor * 0.05f;
            extraAngle = Mth.clamp(extraAngle, -15, 15);

            float currentRelativeYaw = Mth.wrapDegrees(segment.getYRot() - prevSegment.getYRot());
            float targetRelativeYaw = extraAngle;
            float newRelativeYaw = currentRelativeYaw + (targetRelativeYaw - currentRelativeYaw) * ANGLE_SMOOTHING;

            float newYaw = prevSegment.getYRot() + Mth.clamp(newRelativeYaw, -MAX_SEGMENT_ANGLE, MAX_SEGMENT_ANGLE);
            segment.setYRot(newYaw);
        }
    }

    private void applySegmentAngleConstraints() {
        for (int i = 1; i < segments.size(); i++) {
            CentipedeJoint segment = segments.get(i);
            CentipedeJoint prevSegment = segments.get(i - 1);
            constrainSegmentAngle(segment, prevSegment);
        }

        if (!segments.isEmpty()) {
            constrainSegmentAngle(segments.get(0), this);
        }
    }

    private void constrainSegmentAngle(CentipedeJoint segment, Entity previousEntity) {
        float currentYaw = segment.getYRot();
        float prevYaw = previousEntity.getYRot();
        float deltaYaw = Mth.wrapDegrees(currentYaw - prevYaw);

        if (Math.abs(deltaYaw) > MAX_SEGMENT_ANGLE) {
            float newYaw = prevYaw + Math.signum(deltaYaw) * MAX_SEGMENT_ANGLE;
            segment.setYRot(newYaw);
        }
    }

    private void applyTailSway() {
        if (segments.size() < 3) return;

        int tailStart = Math.max(0, segments.size() - 3);
        float time = tickCount * 0.1f;

        for (int i = tailStart; i < segments.size(); i++) {
            CentipedeJoint segment = segments.get(i);
            float tailFactor = (float) (i - tailStart) / (segments.size() - tailStart);
            float swayAmplitude = 5.0f * tailFactor;

            if (swayAmplitude > 0.1f) {
                float sway = (float) Math.sin(time + i * 0.5) * swayAmplitude;
                Entity prevEntity = (i > 0) ? segments.get(i - 1) : this;
                float prevYaw = prevEntity.getYRot();
                float currentYaw = segment.getYRot();
                float relativeYaw = Mth.wrapDegrees(currentYaw - prevYaw);
                float targetRelativeYaw = relativeYaw + sway * 0.3f;
                float newRelativeYaw = Mth.clamp(targetRelativeYaw, -MAX_SEGMENT_ANGLE, MAX_SEGMENT_ANGLE);
                float newYaw = prevYaw + newRelativeYaw;
                float smoothedYaw = currentYaw + (newYaw - currentYaw) * 0.3f;
                segment.setYRot(smoothedYaw);
            }
        }
    }

    // ==================== 属性更新 ====================

    private void updateAttackDamageBasedOnHealth() {
        float maxHealth = this.entityData.get(DATA_MAX_HEALTH);
        int segmentCount = (int) Math.ceil(maxHealth / getBaseHealthPerSegment());

        float newAttackDamage = BASE_ATTACK_DAMAGE + (segmentCount * ATTACK_BONUS_PER_SEGMENT);

        float currentAttackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Math.abs(currentAttackDamage - newAttackDamage) > 0.1f) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newAttackDamage);
        }
    }

    // ==================== 节段管理 ====================

    private void ensureChainIntegrity() {
        if (segments.isEmpty()) return;

        boolean needsReconnect = false;

        if (segments.get(0).getPreviousEntity() != this) {
            needsReconnect = true;
        }

        for (int i = 1; i < segments.size(); i++) {
            if (segments.get(i).getPreviousEntity() != segments.get(i - 1)) {
                needsReconnect = true;
                break;
            }
        }

        if (needsReconnect) {
            reconnectBrokenChain();
        }
    }

    private void deleteAllOldSegments() {
        if (level() instanceof ServerLevel serverLevel) {
            for (UUID uuid : segmentUUIDs) {
                Entity entity = serverLevel.getEntity(uuid);
                if (entity != null && entity.isAlive()) {
                    entity.discard();
                }
            }
        }
        segments.clear();
        segmentUUIDs.clear();
    }

    private void growNewSegment() {
        if (level().isClientSide) return;

        float currentMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
        float newMaxHealth = currentMaxHealth + getBaseHealthPerSegment();
        this.entityData.set(DATA_MAX_HEALTH, newMaxHealth);
        this.entityData.set(DATA_TOTAL_HEALTH, newMaxHealth);

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        this.setHealth(newMaxHealth);

        CentipedeJoint newSegment = (CentipedeJoint) createSegment(segments.size());

        Entity previousEntity = this;
        if (!segments.isEmpty()) {
            previousEntity = segments.get(segments.size() - 1);
        }
        newSegment.setPreviousEntity(previousEntity);

        Vec3 prevPos = previousEntity.position();
        float prevYaw = previousEntity.getYRot();
        double yawRad = Math.toRadians(prevYaw + 180);
        double distance = 2.1;
        double newX = prevPos.x - Math.sin(yawRad) * distance;
        double newZ = prevPos.z + Math.cos(yawRad) * distance;
        newSegment.moveTo(newX, prevPos.y, newZ);

        newSegment.setYRot(prevYaw);

        addSegment(newSegment);
        segments.add(newSegment);

        syncSegmentUUIDs();
        this.entityData.set(DATA_SEGMENT_COUNT, segments.size());

        logSegmentInfo("增长新节段");
    }

    private void reconnectBrokenChain() {
        if (segments.isEmpty()) return;

        Entity previousEntity = this;
        for (CentipedeJoint segment : segments) {
            segment.setPreviousEntity(previousEntity);
            previousEntity = segment;
        }
    }

    private void syncSegmentUUIDs() {
        this.segmentUUIDs.clear();
        for (CentipedeJoint segment : segments) {
            this.segmentUUIDs.add(segment.getUUID());
        }
        this.entityData.set(DATA_SEGMENT_COUNT, segmentUUIDs.size());
    }

    private void logSegmentInfo(String action) {
        org.apache.logging.log4j.LogManager.getLogger().info(
                "[CentipedeHead] " + action + " | 节段数: " + segments.size() +
                        " | 血量: " + getTotalHealth() + "/" + getMaxTotalHealth()
        );
    }

    public void initializeSegments(int segmentCount) {
        if (!level().isClientSide && segments.isEmpty()) {
            float initialHealth = segmentCount * getBaseHealthPerSegment();
            this.entityData.set(DATA_MAX_HEALTH, initialHealth);
            this.entityData.set(DATA_TOTAL_HEALTH, initialHealth);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(initialHealth);
            this.setHealth(initialHealth);

            List<CentipedeJoint> newSegments = new ArrayList<>();
            Entity previousEntity = this;

            for (int i = 0; i < segmentCount; i++) {
                CentipedeJoint segment = (CentipedeJoint) createSegment(i);
                segment.setPreviousEntity(previousEntity);

                Vec3 prevPos = previousEntity.position();
                float prevYaw = previousEntity.getYRot();
                double yawRad = Math.toRadians(prevYaw + 180);
                double distance = 2.1;
                double newX = prevPos.x - Math.sin(yawRad) * distance;
                double newZ = prevPos.z + Math.cos(yawRad) * distance;

                segment.moveTo(newX, prevPos.y, newZ);
                segment.setYRot(prevYaw + (i % 2 == 0 ? 5 : -5));

                newSegments.add(segment);
                previousEntity = segment;
            }

            for (CentipedeJoint segment : newSegments) {
                addSegment(segment);
                segments.add(segment);
            }

            this.entityData.set(DATA_SEGMENT_COUNT, segmentCount);
            logSegmentInfo("创建了 " + segmentCount + " 个节段");
        }
    }

    @Override
    protected float getBaseHealthPerSegment() {
        return 30.0f;
    }

    @Override
    public AbstractSegmentedJoint createSegment(int index) {
        return new CentipedeJoint(EntityRegistry.CENTIPEDE_JOINT.get(), level());
    }

    @Override
    public List<? extends AbstractSegmentedJoint> getSegments() {
        return this.segments;
    }

    // ==================== Geckolib 动画 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        AnimationController<CentipedeHead> mainController = new AnimationController<>(this, "mainController", 5, this::animationController);
        AnimationController<CentipedeHead> mouthController = new AnimationController<>(this, "mouthController", 2, this::mouthAnimationController);
        controllerRegistrar.add(mainController, mouthController);
    }

    private <E extends CentipedeHead> PlayState animationController(AnimationState<E> event) {
        if (this.entityData.get(DATA_PLAYING_ATTACK)) {
            return event.setAndContinue(ATTACK_ANIM);
        }

        if (event.isMoving() && !isUnderground && !isBurrowed) {
            return event.setAndContinue(RUN_ANIM);
        }

        return PlayState.STOP;
    }

    private <E extends CentipedeHead> PlayState mouthAnimationController(AnimationState<E> event) {
        if (this.entityData.get(DATA_MOUTH_OPEN)) {
            return event.setAndContinue(MOUTH_OPEN_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 声音 ====================

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isUnderground || isBurrowed ? null : SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isUnderground && !isBurrowed) {
            this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
        }
    }

    // ==================== 继承方法 ====================

    @Override
    protected void updateChildSegmentsList(List<AbstractSegmentedJoint> validSegments) {
        this.segments.clear();
        for (AbstractSegmentedJoint segment : validSegments) {
            if (segment instanceof CentipedeJoint centipedeJoint) {
                this.segments.add(centipedeJoint);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        hungerSystem.loadData(compound);
        this.segmentsInitialized = false;
        this.isUnderground = compound.getBoolean("IsUnderground");
        this.isBurrowed = compound.getBoolean("IsBurrowed");
        this.undergroundTimer = compound.getInt("UndergroundTimer");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        hungerSystem.saveData(compound);
        compound.putBoolean("SegmentsInitialized", this.segmentsInitialized);
        compound.putBoolean("IsUnderground", this.isUnderground);
        compound.putBoolean("IsBurrowed", this.isBurrowed);
        compound.putInt("UndergroundTimer", this.undergroundTimer);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!level().isClientSide) {
            dropLootBasedOnHealth();
            for (CentipedeJoint segment : segments) {
                if (segment.isAlive()) {
                    segment.discard();
                }
            }
        }
        super.die(damageSource);
    }

    private void dropLootBasedOnHealth() {
        float maxHealth = this.entityData.get(DATA_MAX_HEALTH);
        int segmentCount = (int) Math.ceil(maxHealth / getBaseHealthPerSegment());

        int eyeCount = this.random.nextInt(segmentCount) + 1;
        for (int i = 0; i < eyeCount; i++) {
            this.spawnAtLocation(Items.SPIDER_EYE);
        }

        if (maxHealth >= 300.0f) {
            spawnAtLocation(Items.ENDER_PEARL, 2 + this.random.nextInt(3));
            spawnAtLocation(Items.GOLDEN_APPLE);
            spawnAtLocation(Items.DIAMOND, 1 + this.random.nextInt(2));
        } else if (maxHealth >= 150.0f) {
            spawnAtLocation(Items.ENDER_PEARL);
            spawnAtLocation(Items.GOLD_INGOT, 2 + this.random.nextInt(3));
        } else if (maxHealth >= 60.0f) {
            spawnAtLocation(Items.IRON_INGOT, 2 + this.random.nextInt(3));
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        triggerAttackAnimation();
        openMouth();

        boolean result = super.doHurtTarget(entity);

        if (result && !this.level().isClientSide) {
            float pitch = 0.8F + this.random.nextFloat() * 0.4F;
            this.playSound(ModSounds.GROUND_CHI.get(), 1.0F, pitch);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        1, 0.1, 0.1, 0.1, 0);
            }
        }

        return result;
    }

    private void triggerAttackAnimation() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_ATTACK, true);
        this.entityData.set(DATA_ATTACK_TICKS, ATTACK_ANIMATION_DURATION);
        this.entityData.set(DATA_ATTACK_COOLDOWN, ATTACK_COOLDOWN_TICKS);

        this.getNavigation().stop();
        this.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void awardKillScore(Entity killed, int scoreValue, DamageSource source) {
        super.awardKillScore(killed, scoreValue, source);
        triggerEatAnimation();

        if (!level().isClientSide && this.random.nextFloat() < 0.2f) {
            growNewSegment();
        }
    }

    @Override
    public float getMaxTotalHealth() {
        return this.entityData.get(DATA_MAX_HEALTH);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return super.canBeAffected(effectInstance);
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

    @Override
    public void aiStep() {
        super.aiStep();
        hungerSystem.tick();
    }
}