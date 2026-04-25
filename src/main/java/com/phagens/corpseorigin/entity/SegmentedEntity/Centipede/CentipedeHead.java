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
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.item.Items;
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

public class CentipedeHead extends AbstractSegmentedHead implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<CentipedeJoint> segments = new ArrayList<>();
    private boolean segmentsInitialized = false;

    // Boss 血条阈值：最大血量超过此值时显示 Boss 血条
    private static final float BOSS_HEALTH_THRESHOLD = 150.0f;
    // 攻击力基础值和每节血量提供的加成
    private static final float BASE_ATTACK_DAMAGE = 6.0f;
    private static final float ATTACK_BONUS_PER_SEGMENT = 1.0f;
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
        if (level().isClientSide) return;

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
                org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 检测到存档数据，删除所有旧节段并重新生成");
                deleteAllOldSegments();
            }
            int expectedCount = calculateExpectedSegmentCount();
            if (segments.isEmpty()) {
                initializeSegments(expectedCount);
            }

            segmentsInitialized = true;
            org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 初始化完成 | 最终节段数: " + segments.size());
        }

        segments.removeIf(segment -> !segment.isAlive());
        ensureChainIntegrity();

        if (!segments.isEmpty()) {
            syncSegmentUUIDs();
        }
    }

    /**
     * 根据最大血量动态调整攻击力
     * 公式：基础攻击力 + (节段数 × 每节加成)
     */
    private void updateAttackDamageBasedOnHealth() {
        float maxHealth = this.entityData.get(DATA_MAX_HEALTH);
        int segmentCount = (int) Math.ceil(maxHealth / getBaseHealthPerSegment());

        float newAttackDamage = BASE_ATTACK_DAMAGE + (segmentCount * ATTACK_BONUS_PER_SEGMENT);

        // 只有当攻击力变化时才更新属性
        float currentAttackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Math.abs(currentAttackDamage - newAttackDamage) > 0.1f) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newAttackDamage);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return super.canBeAffected(effectInstance);
    }

    /**
     * 确保链条完整性：检查每个节段的前一节是否正确
     */
    private void ensureChainIntegrity() {
        if (segments.isEmpty()) return;

        boolean needsReconnect = false;

        // 检查第一节是否指向头部
        if (segments.get(0).getPreviousEntity() != this) {
            needsReconnect = true;
        }

        // 检查后续节段是否指向前一节
        for (int i = 1; i < segments.size(); i++) {
            if (segments.get(i).getPreviousEntity() != segments.get(i - 1)) {
                needsReconnect = true;
                break;
            }
        }

        // 只有在链条断裂时才重连
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
                    org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 已删除旧节段: " + uuid);
                }
            }
        }
        segments.clear();
        segmentUUIDs.clear();
    }

    @Override
    public void awardKillScore(Entity killed, int scoreValue, DamageSource source) {
        super.awardKillScore(killed, scoreValue, source);

        if (!level().isClientSide && this.random.nextFloat() < 0.2f) {
            growNewSegment();
        }
    }

    private void growNewSegment() {
        if (level().isClientSide) return;

        float currentMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
        float newMaxHealth = currentMaxHealth + getBaseHealthPerSegment();
        this.entityData.set(DATA_MAX_HEALTH, newMaxHealth);
        this.entityData.set(DATA_TOTAL_HEALTH, newMaxHealth);

        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        this.setHealth(newMaxHealth);

        CentipedeJoint newSegment = (CentipedeJoint) createSegment(segments.size());

        Entity previousEntity = this;
        if (!segments.isEmpty()) {
            previousEntity = segments.get(segments.size() - 1);
        }
        newSegment.setPreviousEntity(previousEntity);

        Vec3 prevPos = previousEntity.position();
        float prevYaw = previousEntity.getYRot();
        double yawRad = Math.toRadians(prevYaw);
        double newX = prevPos.x + Math.sin(yawRad) * 1.5;
        double newZ = prevPos.z - Math.cos(yawRad) * 1.5;
        newSegment.moveTo(newX, prevPos.y, newZ);

        addSegment(newSegment);
        segments.add(newSegment);

        org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 成长成功！当前节段数: " + segments.size() + ", 最大血量: " + newMaxHealth);
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

    public void initializeSegments(int segmentCount) {
        if (!level().isClientSide && segments.isEmpty()) {
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
                segment.setPreviousEntity(previousEntity);
                double offset = (i + 1) * 1.5;
                segment.moveTo(startX, startY, startZ - offset);
                newSegments.add(segment);
                previousEntity = segment;
            }
            for (CentipedeJoint segment : newSegments) {
                addSegment(segment);
                segments.add(segment);
            }

            this.entityData.set(DATA_SEGMENT_COUNT, segmentCount);

            org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 创建了 " + segmentCount + " 个节段，初始血量: " + initialHealth);
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
        this.segmentsInitialized = false;
        org.apache.logging.log4j.LogManager.getLogger().info("[CentipedeHead] 读档完成，重置状态 | 保存的血量: " + getTotalHealth());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
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

    /**
     * 根据最大血量分级掉落战利品
     */
    private void dropLootBasedOnHealth() {
        float maxHealth = this.entityData.get(DATA_MAX_HEALTH);
        int segmentCount = (int) Math.ceil(maxHealth / getBaseHealthPerSegment());

        // 基础掉落：每个节段概率掉落蜘蛛眼
        int eyeCount = this.random.nextInt(segmentCount) + 1;
        for (int i = 0; i < eyeCount; i++) {
            this.spawnAtLocation(Items.SPIDER_EYE);
        }

        // 分级掉落逻辑
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
    public float getMaxTotalHealth() {
        return this.entityData.get(DATA_MAX_HEALTH);
    }
}
