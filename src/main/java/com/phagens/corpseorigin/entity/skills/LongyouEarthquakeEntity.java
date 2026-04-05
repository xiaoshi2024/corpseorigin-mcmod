package com.phagens.corpseorigin.entity.skills;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.LongyouEarthquakeBlockPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LongyouEarthquakeEntity extends Entity {

    private static final EntityDataAccessor<Vector3f> CENTER = SynchedEntityData.defineId(LongyouEarthquakeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(LongyouEarthquakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(LongyouEarthquakeEntity.class, EntityDataSerializers.INT);

    // 客户端存储翻动方块的实例
    private final Map<BlockPos, EarthquakeBlockInstance> blockInstances = new HashMap<>();

    // 服务端存储已生成的半径
    private int previousRadius = -1;
    private int tickCount = 0;

    public LongyouEarthquakeEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public static LongyouEarthquakeEntity create(Level level, Vec3 center, double radius, int duration) {
        LongyouEarthquakeEntity earthquake = new LongyouEarthquakeEntity(EntityRegistry.LONGYOU_EARTHQUAKE.get(), level);
        earthquake.entityData.set(CENTER, new Vector3f((float) center.x, (float) center.y, (float) center.z));
        earthquake.entityData.set(RADIUS, (float) radius);
        earthquake.entityData.set(DURATION, duration);
        earthquake.setPos(center);
        level.addFreshEntity(earthquake);
        return earthquake;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CENTER, new Vector3f(0, 0, 0));
        builder.define(RADIUS, 8.0F);
        builder.define(DURATION, 40);
    }

    @Override
    public void tick() {
        super.tick();

        tickCount++;

        if (level().isClientSide) {
            // 客户端：更新翻动方块动画
            updateBlockInstances();
        } else {
            // 服务端：生成地震波扩散效果
            updateServerSide();
        }
    }

    private void updateServerSide() {
        float radius = getRadius();
        float currentRadius = (float) tickCount / getDuration() * radius;

        int currentRadInt = (int) currentRadius;

        // 随着半径扩大，生成新的翻动方块
        if (currentRadInt != previousRadius) {
            for (int r = previousRadius + 1; r <= currentRadInt; r++) {
                spawnBlocksAtRadius(r);
            }
            previousRadius = currentRadInt;
        }

        // 播放音效
        if (tickCount % 5 == 0) {
            ServerLevel serverLevel = (ServerLevel) level();
            Vector3f center = getCenter();
            serverLevel.playSound(null, center.x(), center.y(), center.z(),
                    SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    2.0F, 0.8F + random.nextFloat() * 0.4f);
        }

        // 伤害实体
        if (tickCount % 2 == 0) {
            damageEntities();
        }

        // 结束地震
        if (tickCount > getDuration()) {
            remove(RemovalReason.DISCARDED);
        }
    }

    // 修改 spawnBlocksAtRadius 方法中的发送逻辑
    // 修改 LongyouEarthquakeEntity.java 中的 spawnBlocksAtRadius 方法
    private void spawnBlocksAtRadius(int radius) {
        Vector3f centerVec = getCenter();
        Vec3 center = new Vec3(centerVec.x(), centerVec.y(), centerVec.z());

        // 计算圆周长上的方块数量
        int blockCount = Math.max(8, (int)(radius * Math.PI * 2 / 1.5));

        for (int i = 0; i < blockCount; i++) {
            double angle = (Math.PI * 2 / blockCount) * i;
            angle += random.nextDouble() * Math.PI / 4;

            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            BlockPos blockPos = new BlockPos(
                    (int) Math.floor(center.x + x),
                    (int) (center.y - 1),
                    (int) Math.floor(center.z + z)
            );

            BlockState blockState = level().getBlockState(blockPos);

            if (blockState.isAir() || blockState.getFluidState().isSource()) {
                BlockPos belowPos = blockPos.below();
                BlockState belowState = level().getBlockState(belowPos);
                if (!belowState.isAir()) {
                    blockPos = belowPos;
                    blockState = belowState;
                } else {
                    BlockPos abovePos = blockPos.above();
                    BlockState aboveState = level().getBlockState(abovePos);
                    if (!aboveState.isAir()) {
                        blockPos = abovePos;
                        blockState = aboveState;
                    } else {
                        continue;
                    }
                }
            }

            Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            Vec3 directionToCenter = center.subtract(blockCenter).normalize();

            directionToCenter = directionToCenter.add(
                    (random.nextDouble() - 0.5) * 0.3,
                    (random.nextDouble() - 0.5) * 0.3,
                    (random.nextDouble() - 0.5) * 0.3
            ).normalize();

            // ✅ 修复：第一个参数应该是 Entity (this)，而不是 serverLevel
            if (level() instanceof ServerLevel) {
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new LongyouEarthquakeBlockPacket(this.getId(), blockPos, blockState, directionToCenter));
            }
        }
    }

    // 客户端接收方块实例
    public void addBlockInstance(BlockPos pos, BlockState state, Vec3 direction) {
        if (!level().isClientSide) return;
        // 避免重复添加
        if (!blockInstances.containsKey(pos)) {
            blockInstances.put(pos, new EarthquakeBlockInstance(pos, state, direction));
//            CorpseOrigin.LOGGER.debug("添加翻动方块: 位置={}, 方块={}", pos, state.getBlock().getDescriptionId());
        }
    }

    private void updateBlockInstances() {
        // 移除过期的实例
        blockInstances.entrySet().removeIf(entry -> {
            entry.getValue().tick();
            return entry.getValue().isFinished();
        });
    }

    private void damageEntities() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Vector3f centerVec = getCenter();
        Vec3 center = new Vec3(centerVec.x(), centerVec.y(), centerVec.z());
        float radius = getRadius();

        // 当前地震波半径
        float currentRadius = (float) tickCount / getDuration() * radius;

        AABB damageBox = new AABB(
                center.x - currentRadius - 1, center.y - 2, center.z - currentRadius - 1,
                center.x + currentRadius + 1, center.y + 3, center.z + currentRadius + 1
        );

        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, damageBox);

        for (LivingEntity entity : entities) {
            if (entity.is(this)) {
                continue;
            }
            
            // 排除龙右的尸兄手下，避免误伤
            if (entity instanceof com.phagens.corpseorigin.entity.LowerLevelZbEntity) {
                continue;
            }

            double dist = entity.position().distanceTo(center);
            // 只有在地震波边缘附近的实体才受到伤害
            if (Math.abs(dist - currentRadius) <= 2.0) {
                // 伤害随半径增加而衰减
                float damage = Math.max(4.0f, 12.0f * (1 - currentRadius / radius));
                entity.hurt(serverLevel.damageSources().explosion(null, null), damage);

                // 震飞效果
                Vec3 pushDir = entity.position().subtract(center).normalize();
                entity.setDeltaMovement(pushDir.x * 0.8, 0.5 + currentRadius / radius, pushDir.z * 0.8);
                entity.hurtMarked = true;
            }
        }
    }

    public Map<BlockPos, EarthquakeBlockInstance> getBlockInstances() {
        return blockInstances;
    }

    public Vector3f getCenter() {
        return entityData.get(CENTER);
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }

    public int getDuration() {
        return entityData.get(DURATION);
    }

    public float getCurrentProgress(float partialTick) {
        return Math.min(1.0f, (tickCount + partialTick) / getDuration());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return true;
    }

    // 方块翻动实例类
    public static class EarthquakeBlockInstance {
        private final BlockPos pos;
        private final BlockState blockState;
        private final Vec3 directionToCenter;
        private final int upTime;
        private final int stayTime;
        private final int downTime;
        private final int totalLifetime;
        private int tickCount = 0;

        public EarthquakeBlockInstance(BlockPos pos, BlockState blockState, Vec3 directionToCenter) {
            this.pos = pos;
            this.blockState = blockState;
            this.directionToCenter = directionToCenter;

            // 随机动画时长，让效果更自然
            this.upTime = 4 + (int)(Math.random() * 4);
            this.stayTime = 3 + (int)(Math.random() * 3);
            this.downTime = 5 + (int)(Math.random() * 4);
            this.totalLifetime = upTime + stayTime + downTime;
        }

        public void tick() {
            tickCount++;
        }

        public boolean isFinished() {
            return tickCount >= totalLifetime;
        }

        public BlockPos getPos() {
            return pos;
        }

        public BlockState getBlockState() {
            return blockState;
        }

        public Vec3 getDirection() {
            return directionToCenter;
        }

        public float getAnimationProgress(float partialTick) {
            float time = tickCount + partialTick;
            float upPhase = upTime;
            float stayPhase = upTime + stayTime;

            if (time < upPhase) {
                // 上升阶段
                return time / upPhase;
            } else if (time < stayPhase) {
                // 停留阶段
                return 1.0f;
            } else {
                // 下落阶段
                float downTimeElapsed = time - stayPhase;
                float downPhase = downTime;
                return Math.max(0, 1.0f - (downTimeElapsed / downPhase));
            }
        }

        public float getHeightOffset(float progress) {
            // 使用缓动函数，让上升和下落更自然
            if (progress < 0.5f) {
                // 上升：从0到1，使用 easeOut 缓动
                float t = progress * 2;
                return (1 - (1 - t) * (1 - t)) * 1.2f;
            } else {
                // 下落：从1到0，使用 easeIn 缓动
                float t = (progress - 0.5f) * 2;
                return (1 - t * t) * 1.2f;
            }
        }

        public float getRotationAngle(float progress) {
            // 旋转角度：从0到最大角度，然后保持，下落时逐渐回正
            if (progress < 0.6f) {
                return progress / 0.6f * 75.0f;
            } else {
                float t = (progress - 0.6f) / 0.4f;
                return 75.0f * (1 - t);
            }
        }
    }
}