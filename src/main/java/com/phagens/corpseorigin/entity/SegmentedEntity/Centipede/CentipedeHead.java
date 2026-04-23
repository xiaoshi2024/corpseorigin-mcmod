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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蜈蚣头部实体
 *
 * 职责：
 * 1. 作为多节段生物的"主控端"，负责AI决策和移动
 * 2. 维护所有身体节段的引用列表
 * 3. 管理总血量系统
 * 4. 处理受伤时的断裂逻辑
 */
public class CentipedeHead extends AbstractSegmentedHead implements GeoEntity {
    //同步节段数量
    private static final EntityDataAccessor<Integer> DATA_SEGMENT_COUNT =
            SynchedEntityData.defineId(CentipedeHead.class, EntityDataSerializers.INT);
    //默认节
    private int segmentCount = 2;
    //身体阶段访问列表
    private final List<CentipedeJoint> segments = new ArrayList<>();
    public CentipedeHead(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.totalMaxHealth = 80.0f;
        this.currentTotalHealth = 80.0f;
    }

    // GeckoLib 动画缓存
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    //需要同步给客户端的数据 初始化自动调用
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SEGMENT_COUNT, 2);
    }
    //AI行为
    @Override
    protected void registerGoals() {
        super.registerGoals();
    }
    //属性注册
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()  // 基于怪物基础属性
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    public void tick() {
        // 客户端不执行游戏逻辑，只负责渲染
        if (level().isClientSide) {
            return;
        }



        // 如果segments列表为空但UUID列表不为空，说明是从存档加载的
        // 需要从UUID重新获取实体引用
        if (segments.isEmpty() && !segmentUUIDs.isEmpty()) {
            loadSegmentsFromUUIDs();
        }

        boolean hasDeadSegment = false;
        for (CentipedeJoint segment : segments) {
            if (!segment.isAlive()) {
                hasDeadSegment = true;
            }
        }

        // 只要有节段死亡，就重新计算总血量
        if (hasDeadSegment) {
            syncHealthToTotal();
        }

        // 头部血量归零时可以添加"虚弱"状态逻辑（可选）
        if (this.currentTotalHealth <= 0 && !this.isRemoved()) {
            this.die(this.damageSources().generic());
        }

        // 防止头部血量过低导致频繁触发die()
        if (this.getHealth() <= 0 && this.currentTotalHealth > 0) {
            this.setHealth(1.0F);
        }

        // 如果头部血量过低，可以恢复一点（模拟再生能力）
        if (this.getHealth() < this.getMaxHealth() * 0.3f && this.tickCount % 100 == 0) {
            this.heal(1.0F);  // 每5秒恢复1点血
        }



    }


    /**
     * 从UUID列表重新加载节段引用
     * 用于世界重载后恢复节段引用
     */
    private void loadSegmentsFromUUIDs() {
        segments.clear();
        if (level() instanceof ServerLevel serverLevel) {
            for (UUID uuid : segmentUUIDs) {
                Entity entity = serverLevel.getEntity(uuid);
                if (entity instanceof CentipedeJoint joint) {
                    segments.add(joint);
                }
            }
        }
    }

    /**
     * 初始化身体节段
     * 在实体生成后立即调用，创建完整的蜈蚣身体
     */
    public void initializeSegments() {
        // 只在服务端执行，且只初始化一次
        if (!level().isClientSide && segments.isEmpty()) {

            // 确保 segmentCount 有默认值
            if (this.segmentCount <= 0) {
                this.segmentCount = 2;
                System.out.println("[CentipedeHead] segmentCount was 0, reset to 2");
            }

            // 获取头部的当前位置作为起始点
            double startX = getX();
            double startY = getY();
            double startZ = getZ();

            // 第一节的"前一节"是头部自己
            UUID previousUUID = this.getUUID();

            // 循环创建指定数量的节段
            for (int i = 0; i < segmentCount; i++) {
                System.out.println("[CentipedeHead] Creating segment " + i);
                // 创建一个新节段
                CentipedeJoint segment = (CentipedeJoint) createSegment(i);

                // 设置它的前一节是谁（链式连接的关键）
                segment.setPreviousUUID(previousUUID);

                // 计算位置：每一节沿Z轴负方向偏移1.5格
                // 这样蜈蚣生成时就是伸展开的
                double offset = (i + 1) * 1.5;
                segment.moveTo(startX, startY, startZ - offset);

                // 将节段添加到世界中
                addSegment(segment);

                // 添加到本地引用列表
                segments.add(segment);

                // 下一节的前一节就是当前这一节
                previousUUID = segment.getUUID();
                System.out.println("[CentipedeHead] Segment " + i + " created successfully");
            }

            // 同步节段数量到客户端
            this.entityData.set(DATA_SEGMENT_COUNT, segmentCount);

            System.out.println("[CentipedeHead] Segments initialized successfully, total: " + segments.size());
        }
    }

    /**
     * 创建单个节段（由基类定义的抽象方法）
     *
     * @param index 节段索引（0-based）
     * @return 新创建的节段实体
     */
    @Override
    public AbstractSegmentedJoint createSegment(int index) {
        return new CentipedeJoint((EntityType<? extends Monster>) (EntityType<?>)
                EntityRegistry.CENTIPEDE_JOINT.get(), level());
    }

    /**
     * 将所有存活的节段血量汇总到总血量
     * 用于保持总血量与实际状态一致
     */
    void syncHealthToTotal() {
        float headCurrentHealth = this.getHealth();
        float totalLocalHealth = 0;

        // 累加所有存活节段的当前血量
        for (CentipedeJoint segment : segments) {
            if (segment.isAlive()) {
                totalLocalHealth += segment.getCurrentLocalHealth();
            }
        }


        // 总血量 = 所有节段血量 + 头部按比例的血量
        this.currentTotalHealth = headCurrentHealth + totalLocalHealth;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 先调用父类的受伤逻辑（扣血、播放音效等）
        boolean result = super.hurt(source, amount);

        // 如果成功受伤且在服务端
        if (result && !level().isClientSide) {
            // 同步总血量
            syncHealthToTotal();

        }

        return result;
    }


    //空闲环境音
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return super.getAmbientSound();
    }
    //受伤音
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return super.getHurtSound(damageSource);
    }
    //死亡音
    @Override
    protected SoundEvent getDeathSound() {
        return super.getDeathSound();
    }
    //走路音 pos位 blockin方块状态
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // 音量调小一点，因为蜈蚣有很多脚
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
    }
//nbt读取存档数据世界重载调用
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // 读取节段数量
        this.segmentCount = compound.getInt("SegmentCount");
        // 读取最大总血量
        this.totalMaxHealth = compound.getFloat("TotalMaxHealth");
    }
//写入NBT存档数据 世界保存调用
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // 保存节段数量
        compound.putInt("SegmentCount", this.segmentCount);
        // 保存最大总血量
        compound.putFloat("TotalMaxHealth", this.totalMaxHealth);
    }
//死亡处理
    @Override
    public void die(DamageSource damageSource) {
        // 只有总血量真正归零时才死亡
        if (this.currentTotalHealth > 0) {
            return;  // 阻止死亡
        }

        if (!level().isClientSide) {
            for (CentipedeJoint segment : segments) {
                if (segment.isAlive()) {
                    segment.discard();
                }
            }
        }

        super.die(damageSource);

        // 确保实体被移除
        this.discard();
    }



//获取节
    public int getSegmentCount() {
        return segmentCount;
    }
//获取节引用列表
    public List<CentipedeJoint> getSegments() {
    return segments;
}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


}
