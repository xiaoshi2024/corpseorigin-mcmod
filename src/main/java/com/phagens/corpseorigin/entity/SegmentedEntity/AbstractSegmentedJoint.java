package com.phagens.corpseorigin.entity.SegmentedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

//身体
public abstract class AbstractSegmentedJoint extends Monster {
    protected Entity previousEntity;

    // 弹簧质点系统属性
    protected Vec3 velocity = Vec3.ZERO;
    protected Vec3 lastPosition = Vec3.ZERO;
    protected Vec3 lastTargetPosition = Vec3.ZERO;

    // 可配置的物理参数
    protected static final float DAMPING = 0.85f;      // 阻尼系数 (0.7-0.95，越小衰减越快)
    protected static final float STIFFNESS = 0.25f;    // 弹簧刚度 (0.15-0.4，越大响应越快)
    protected static final float MAX_SPEED = 1.2f;     // 最大移动速度
    protected static final float MAX_MOVE_DISTANCE = 0.8f; // 单次最大移动距离
    protected static final float MIN_MOVE_THRESHOLD = 0.01f; // 最小移动阈值

    // 平滑参数
    protected static final float POSITION_SMOOTHING = 0.6f;  // 位置平滑系数 (0.3-0.8)
    
    // 飞行台阶效果参数
    protected static final float FLYING_STEP_HEIGHT = 0.35f;  // 每个节段的台阶高度
    protected static final float GROUND_CHECK_DISTANCE = 2.0f; // 判断是否在地面的距离
    
    protected int segmentIndex = 0;

    public AbstractSegmentedJoint(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoAi(true); // 禁用AI
        this.setPersistenceRequired(); // 确保节段不会被自动移除
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端不执行游戏逻辑
        if (level().isClientSide) return;

        // 更新位置记录（用于计算运动方向）
        Vec3 currentPos = this.position();
        if (lastPosition == Vec3.ZERO) {
            lastPosition = currentPos;
        }

        // 跟随前一节移动
        if (previousEntity != null && previousEntity.isAlive()) {
            followEntityWithPhysics(previousEntity);
        } else if (previousEntity != null && !previousEntity.isAlive()) {
            // 前一节已死亡，让这个节段也逐渐停下并消失
            handleOrphanSegment();
        }

        // 更新最后位置
        lastPosition = this.position();
    }

    /**
     * 使用弹簧质点系统进行平滑跟随（核心改进）
     */
    protected void followEntityWithPhysics(Entity target) {
        Vec3 targetPos = target.position();
        Vec3 myPos = this.position();

        // 获取目标的朝向，计算期望的后方位置
        float targetYaw = target.getYRot();
        double angle = Math.toRadians(targetYaw + 180);
        double distance = getSegmentDistance();

        // 基础期望位置（正后方）
        Vec3 baseDesiredPos = new Vec3(
                targetPos.x - Math.sin(angle) * distance,
                targetPos.y,
                targetPos.z + Math.cos(angle) * distance
        );

        // 飞行台阶效果：当蜈蚣不在地面时，节段像台阶一样排列
        if (!this.onGround() && (target instanceof LivingEntity && !((LivingEntity) target).onGround())) {
            int index = getSegmentIndex();
            baseDesiredPos = new Vec3(
                    baseDesiredPos.x,
                    targetPos.y - (index * FLYING_STEP_HEIGHT),
                    baseDesiredPos.z
            );
        }

        // 对期望位置进行平滑处理，减少抖动
        Vec3 desiredPos;
        if (lastTargetPosition == Vec3.ZERO) {
            desiredPos = baseDesiredPos;
        } else {
            // 使用线性插值平滑目标位置
            double smoothX = lastTargetPosition.x + (baseDesiredPos.x - lastTargetPosition.x) * POSITION_SMOOTHING;
            double smoothY = lastTargetPosition.y + (baseDesiredPos.y - lastTargetPosition.y) * POSITION_SMOOTHING;
            double smoothZ = lastTargetPosition.z + (baseDesiredPos.z - lastTargetPosition.z) * POSITION_SMOOTHING;
            desiredPos = new Vec3(smoothX, smoothY, smoothZ);
        }
        lastTargetPosition = desiredPos;

        // 弹簧力：拉向期望位置
        Vec3 springForce = desiredPos.subtract(myPos).scale(STIFFNESS);

        // 添加轻微的水平摆动，让节段看起来更自然（可选）
        if (shouldAddSway()) {
            springForce = addNaturalSway(springForce);
        }

        // 应用阻尼更新速度
        velocity = velocity.add(springForce).scale(DAMPING);

        // 限制最大速度
        if (velocity.length() > MAX_SPEED) {
            velocity = velocity.scale(MAX_SPEED / velocity.length());
        }

        // 应用速度移动
        Vec3 newPos = myPos.add(velocity);

        // 限制最大移动距离，避免穿透
        double moveDistance = newPos.distanceTo(myPos);
        if (moveDistance > MAX_MOVE_DISTANCE) {
            Vec3 direction = newPos.subtract(myPos).normalize();
            newPos = myPos.add(direction.scale(MAX_MOVE_DISTANCE));
        }

        // 确保 Y 轴不会过分偏离（保持在地上）
        newPos = new Vec3(newPos.x, Math.max(newPos.y, targetPos.y - 1.5), newPos.z);

        this.setPos(newPos.x, newPos.y, newPos.z);

        // 更新旋转，使节段朝向移动方向
        updateRotationFromMovement();
    }
    
    protected int getSegmentIndex() {
        if (previousEntity instanceof AbstractSegmentedJoint) {
            return ((AbstractSegmentedJoint) previousEntity).getSegmentIndex() + 1;
        }
        return 1;
    }

    /**
     * 处理孤儿节段（前一节已死亡）
     */
    protected void handleOrphanSegment() {
        // 逐渐减速并消失
        velocity = velocity.scale(0.95f);

        if (velocity.length() < 0.05 && tickCount % 20 == 0) {
            // 如果几乎不动了，且前一节已经死亡一段时间，就消失
            this.discard();
        } else {
            // 继续惯性移动
            Vec3 newPos = this.position().add(velocity);
            this.setPos(newPos.x, newPos.y, newPos.z);
        }
    }

    /**
     * 判断是否应该添加摆动效果（只在快速移动时）
     */
    protected boolean shouldAddSway() {
        return velocity.length() > 0.3;
    }

    /**
     * 添加自然的横向摆动（模拟生物运动）
     */
    protected Vec3 addNaturalSway(Vec3 force) {
        // 使用 tickCount 产生周期性摆动
        double swayX = Math.sin(tickCount * 0.15) * 0.08;
        double swayZ = Math.cos(tickCount * 0.12) * 0.08;

        // 摆动只在横向生效，不影响主方向运动
        return force.add(swayX, 0, swayZ);
    }

    /**
     * 根据运动方向更新节段的朝向
     */
    protected void updateRotationFromMovement() {
        Vec3 movement = this.position().subtract(lastPosition);

        // 使用平滑后的移动方向，避免突然转向
        if (movement.horizontalDistance() > MIN_MOVE_THRESHOLD) {
            // 计算偏航角
            float yaw = (float) (Math.atan2(movement.z, movement.x) * 180.0D / Math.PI) - 90.0F;

            // 计算俯仰角（根据垂直运动）
            float pitch = 0;
            if (movement.length() > 0.1) {
                pitch = (float) Math.asin(Math.max(-1, Math.min(1, movement.y / movement.length()))) * (180.0F / (float) Math.PI);
                pitch = Mth.clamp(pitch, -30, 30); // 限制俯仰角度范围
            }

            // 平滑旋转（避免瞬间转向）
            float currentYaw = this.getYRot();
            float deltaYaw = Mth.wrapDegrees(yaw - currentYaw);

            // 限制每帧最大旋转角度
            float maxRotateSpeed = 15.0f;
            if (Math.abs(deltaYaw) > maxRotateSpeed) {
                yaw = currentYaw + Math.signum(deltaYaw) * maxRotateSpeed;
            }

            this.setRot(yaw, pitch);
        }
    }

    /**
     * 获取相邻节段的固定间距（由子类定义）
     */
    public abstract double getSegmentDistance();

    /**
     * 设置物理参数（可供子类调整）
     */
    public void setPhysicsParams(float damping, float stiffness) {
        // 这个方法留给子类重写，以便调整物理特性
    }

    /**
     * 设置前一节实体引用
     */
    public void setPreviousEntity(Entity entity) {
        this.previousEntity = entity;
        // 重置物理状态，避免突然跳跃
        this.velocity = Vec3.ZERO;
        this.lastTargetPosition = Vec3.ZERO;
    }

    /**
     * 获取前一节实体
     */
    public Entity getPreviousEntity() {
        return this.previousEntity;
    }

    /**
     * 获取当前速度（用于调试）
     */
    public Vec3 getVelocity() {
        return velocity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // 读档后重置物理状态
        velocity = Vec3.ZERO;
        lastPosition = Vec3.ZERO;
        lastTargetPosition = Vec3.ZERO;
    }
}