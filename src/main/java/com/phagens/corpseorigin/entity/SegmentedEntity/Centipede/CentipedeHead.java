package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede;

import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedHead;
import com.phagens.corpseorigin.entity.SegmentedEntity.AbstractSegmentedJoint;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蜈蚣头部实体 - 敌对生物主控端
 */
public class CentipedeHead extends AbstractSegmentedHead implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<CentipedeJoint> segments = new ArrayList<>();
    private boolean segmentsInitialized = false;
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
        // 行为目标
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // 攻击目标
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, true));
    }
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (!segmentsInitialized) {
            org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] segmentsInitialized=false, segmentUUIDs.size=" + segmentUUIDs.size() + ", segments.size=" + segments.size());

            // 如果有保存的UUID列表，说明是读档恢复，需要重建引用
            if (!segmentUUIDs.isEmpty()) {
                segments.clear();
                org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 尝试重建节段引用...");
                rebuildSegmentReferences();
                org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 重建后 segments.size=" + segments.size());
                int expectedCount = calculateExpectedSegmentCount();
                if (segments.size() != expectedCount) {
                    org.apache.logging.log4j.LogManager.getLogger().warn("[CentipedeHead] 节段数量不匹配！期望: " + expectedCount + ", 实际: " + segments.size());
                    // 如果数量不对，强制重新初始化
                    segments.clear();
                    for (AbstractSegmentedJoint joint : segments) {
                        joint.discard();
                    }
                    initializeSegments();
                }
            } else if (segments.isEmpty()) {
                // 否则是新生成，初始化节段
                org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 初始化新节段...");
                initializeSegments();
                org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 初始化后 segments.size=" + segments.size());
            }

            // 标记为已初始化
            segmentsInitialized = true;
        }

        // 清理已死亡的节段引用
        segments.removeIf(segment -> !segment.isAlive());
        reconnectBrokenChain();


        if (!segments.isEmpty()) {
            syncSegmentUUIDs();
        }
    }

    @Override
    public void awardKillScore(Entity killed, int scoreValue, DamageSource source) {
        super.awardKillScore(killed, scoreValue, source);

        if (!level().isClientSide && this.random.nextFloat() < 0.2f) {
            // 20%概率成长
            growNewSegment();
        }
    }

    /**
     * 成长新的一节身体
     */
    private void growNewSegment() {
        if (level().isClientSide) return;

        // 增加总血量
        float currentMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
        float newMaxHealth = currentMaxHealth + getBaseHealthPerSegment();
        this.entityData.set(DATA_MAX_HEALTH, newMaxHealth);

        // 恢复满血
        this.entityData.set(DATA_TOTAL_HEALTH, newMaxHealth);

        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        this.setHealth(newMaxHealth);
        // 在尾部添加新节段
        CentipedeJoint newSegment = (CentipedeJoint) createSegment(segments.size());

        // 设置前一节为当前最后一节
        Entity previousEntity = this;
        if (!segments.isEmpty()) {
            previousEntity = segments.get(segments.size() - 1);
        }
        newSegment.setPreviousEntity(previousEntity);

        // 放置在最后一节的后方
        Vec3 prevPos = previousEntity.position();
        float prevYaw = previousEntity.getYRot();
        double yawRad = Math.toRadians(prevYaw);
        double newX = prevPos.x + Math.sin(yawRad) * 1.5;
        double newZ = prevPos.z - Math.cos(yawRad) * 1.5;
        newSegment.moveTo(newX, prevPos.y, newZ);

        // 添加到世界和列表
        addSegment(newSegment);
        segments.add(newSegment);

        org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 成长成功！当前节段数: " + segments.size() + ", 最大血量: " + newMaxHealth);
    }

    /**
     * 重连断裂的链条
     */
    private void reconnectBrokenChain() {
        if (segments.isEmpty()) return;

        Entity previousEntity = this;
        for (int i = 0; i < segments.size(); i++) {
            CentipedeJoint segment = segments.get(i);
            if (segment.getPreviousEntity() != previousEntity) {
                segment.setPreviousEntity(previousEntity);
            }
            previousEntity = segment;
        }
    }
    /**
     * 同步节段引用列表到UUID列表
     */
    private void syncSegmentUUIDs() {
        this.segmentUUIDs.clear();
        for (CentipedeJoint segment : segments) {
            this.segmentUUIDs.add(segment.getUUID());
        }
        this.entityData.set(DATA_SEGMENT_COUNT, segmentUUIDs.size());
    }

    /**
     * 初始化身体节段
     */
    public void initializeSegments() {
        if (!level().isClientSide && segments.isEmpty()) {
            int segmentCount = 3; // 应该是5

            float initialHealth = segmentCount * getBaseHealthPerSegment();
            this.entityData.set(DATA_MAX_HEALTH, initialHealth);
            this.entityData.set(DATA_TOTAL_HEALTH, initialHealth);

            this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(initialHealth);
            this.setHealth(initialHealth);

            double startX = getX();
            double startY = getY();
            double startZ = getZ();
            List<CentipedeJoint> newSegments = new ArrayList<>();
            Entity previousEntity = this;

            for (int i = 0; i < segmentCount; i++) {
                CentipedeJoint segment = (CentipedeJoint) createSegment(i);

                // 设置前一节是谁
                segment.setPreviousEntity(previousEntity);

                // 计算位置：每一节沿Z轴负方向偏移1.5格
                double offset = (i + 1) * 1.5;
                segment.moveTo(startX, startY, startZ - offset);

                // 临时存储
                newSegments.add(segment);

                // 下一节的前一节就是当前这一节
                previousEntity = segment;
            }

            // 所有节段创建完成后，再统一添加到世界
            for (CentipedeJoint segment : newSegments) {
                addSegment(segment);
                segments.add(segment);
            }

            this.entityData.set(DATA_SEGMENT_COUNT, segmentCount);
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

    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ========== 音效系统 ==========

    /**
     * 空闲环境音（蜘蛛叫声）
     */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    /**
     * 受伤音效
     */
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SPIDER_HURT;
    }

    /**
     * 死亡音效
     */
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    /**
     * 脚步音效（音量调小，因为蜈蚣有很多脚）
     */
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
        // 读档时重置标志位，允许重新初始化
        this.segmentsInitialized = false;
        org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 读档完成，segmentsInitialized=false, segmentUUIDs.size=" + segmentUUIDs.size());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("SegmentsInitialized", this.segmentsInitialized);
    }

    /**
     * 死亡处理
     * 流程：清理所有节段 → 调用父类死亡逻辑 → 销毁自身
     */
    @Override
    public void die(DamageSource damageSource) {
        if (!level().isClientSide) {
            // 销毁所有存活节段
            for (CentipedeJoint segment : segments) {
                if (segment.isAlive()) {
                    segment.discard();
                }
            }
        }
        super.die(damageSource);
        this.discard();
    }



    @Override
    public float getMaxTotalHealth() {
        return this.entityData.get(DATA_MAX_HEALTH);
    }
}
