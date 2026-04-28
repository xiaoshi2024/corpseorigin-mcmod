package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede;

import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedHead;
import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedJoint;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
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

    // 修复：使用正确的动画名称
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("gtwo");

    private int attackAnimationTimer = 0;
    private static final int ATTACK_ANIMATION_DURATION = 15;
    private boolean isAttacking = false;

    // 攻击力基础值和每节血量提供的加成
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

    private float lastHeadYaw = 0;
    private Vec3 lastHeadPos = Vec3.ZERO;

    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);

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
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, true));
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

        // 更新攻击动画计时器
        if (attackAnimationTimer > 0) {
            attackAnimationTimer--;
            if (attackAnimationTimer <= 0) {
                isAttacking = false;
            }
        }
    }

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

        lastHeadPos = currentPos;
        lastHeadYaw = currentYaw;
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

    private void updateAttackDamageBasedOnHealth() {
        float maxHealth = this.entityData.get(DATA_MAX_HEALTH);
        int segmentCount = (int) Math.ceil(maxHealth / getBaseHealthPerSegment());

        float newAttackDamage = BASE_ATTACK_DAMAGE + (segmentCount * ATTACK_BONUS_PER_SEGMENT);

        float currentAttackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Math.abs(currentAttackDamage - newAttackDamage) > 0.1f) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newAttackDamage);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return super.canBeAffected(effectInstance);
    }

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

    @Override
    public void awardKillScore(Entity killed, int scoreValue, DamageSource source) {
        super.awardKillScore(killed, scoreValue, source);

        // 20% 概率增长新节段
        if (!level().isClientSide && this.random.nextFloat() < 0.2f) {
            growNewSegment();
        }
    }

    /**
     * 增长新节段 - 尾巴增加系统
     */
    private void growNewSegment() {
        if (level().isClientSide) return;

        float currentMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
        float newMaxHealth = currentMaxHealth + getBaseHealthPerSegment();
        this.entityData.set(DATA_MAX_HEALTH, newMaxHealth);
        this.entityData.set(DATA_TOTAL_HEALTH, newMaxHealth);

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        this.setHealth(newMaxHealth);

        // 创建新节段
        CentipedeJoint newSegment = (CentipedeJoint) createSegment(segments.size());

        // 确定前一节（尾部最后一节或头部）
        Entity previousEntity = this;
        if (!segments.isEmpty()) {
            previousEntity = segments.get(segments.size() - 1);
        }
        newSegment.setPreviousEntity(previousEntity);

        // 计算新节段的位置（在最后一节后方）
        Vec3 prevPos = previousEntity.position();
        float prevYaw = previousEntity.getYRot();
        double yawRad = Math.toRadians(prevYaw + 180);
        double distance = 2.1;  // 与 getSegmentDistance() 保持一致
        double newX = prevPos.x - Math.sin(yawRad) * distance;
        double newZ = prevPos.z + Math.cos(yawRad) * distance;
        newSegment.moveTo(newX, prevPos.y, newZ);

        // 设置角度跟随前一节
        newSegment.setYRot(prevYaw);

        // 添加到世界和列表
        addSegment(newSegment);
        segments.add(newSegment);

        // 更新数据同步
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        // 修复：创建动画控制器，正确触发攻击动画
        AnimationController<CentipedeHead> controller = new AnimationController<>(this, "controller", 2, event -> {
            // 攻击动画优先级最高
            if (attackAnimationTimer > 0) {
                // 每次攻击都重新触发动画
                event.getController().setAnimation(ATTACK_ANIM);
                return PlayState.CONTINUE;
            }

            // 移动动画
            if (event.isMoving()) {
                return event.setAndContinue(RUN_ANIM);
            }

            return PlayState.STOP;
        });

        controllerRegistrar.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
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
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
    }

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
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        hungerSystem.saveData(compound);
        compound.putBoolean("SegmentsInitialized", this.segmentsInitialized);
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
        // 修复：触发攻击动画
        attackAnimationTimer = ATTACK_ANIMATION_DURATION;
        this.swing(InteractionHand.MAIN_HAND);
        return super.doHurtTarget(entity);
    }

    @Override
    public float getMaxTotalHealth() {
        return this.entityData.get(DATA_MAX_HEALTH);
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

    @Override
    public void aiStep() {
        super.aiStep();
        hungerSystem.tick();
    }
}