package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.api.infection.EntityInfectionRegistry;
import com.phagens.corpseorigin.api.infection.InfectionAPI;
import com.phagens.corpseorigin.api.infection.InfectionEvent;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 尸体残肢实体
 *
 * 【功能说明】
 * 完全参照 Mob-Dismemberment 1.12.2 的 EntityGib 设计
 * 合并尸体和残肢为一个实体类型
 *
 * 【类型定义】
 * 0 = 头部 (head)
 * 1 = 左臂 (left arm)
 * 2 = 右臂 (right arm)
 * 3 = 身体 (body) - 可被感染
 * 4 = 左腿 (left leg)
 * 5 = 右腿 (right leg)
 * 6+ = 苦力怕脚 (creeper feet)
 *
 * 【感染机制】
 * 只有 type 3 (身体) 可以被感染变成僵尸
 */
public class CorpseGibEntity extends Entity {

    // 同步数据
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(CorpseGibEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_PARENT_TYPE = SynchedEntityData.defineId(CorpseGibEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_SKELETON = SynchedEntityData.defineId(CorpseGibEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GROUND_TIME = SynchedEntityData.defineId(CorpseGibEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_INFECTION_PROGRESS = SynchedEntityData.defineId(CorpseGibEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_RANDOM_TEXTURE_INDEX = SynchedEntityData.defineId(CorpseGibEntity.class, EntityDataSerializers.INT);

    // 父实体引用 (客户端用)
    @Nullable
    private LivingEntity parent;
    private UUID parentUUID;

    // 物理属性
    public float pitchSpin;
    public float yawSpin;
    public int liveTime;
    public boolean explosion;

    // 常量
    public static final int GIB_TYPE_HEAD = 0;
    public static final int GIB_TYPE_LEFT_ARM = 1;
    public static final int GIB_TYPE_RIGHT_ARM = 2;
    public static final int GIB_TYPE_BODY = 3;  // 可被感染
    public static final int GIB_TYPE_LEFT_LEG = 4;
    public static final int GIB_TYPE_RIGHT_LEG = 5;
    public static final int GIB_TYPE_CREEPER_FEET_START = 6;

    // 配置
    private static final int MAX_LIFETIME = 6000; // 5分钟 = 6000 ticks（总生命周期）
    public static final int GROUND_TIME_BEFORE_FADE = 0; // 落地后立即开始变透明
    public static final int GROUND_TIME_FADE_DURATION = 6000; // 变透明持续5分钟(6000 ticks)后消失
    public static final int GROUND_TIME_MAX = GROUND_TIME_FADE_DURATION; // 落地后6000 ticks(5分钟)消失
    private static final float INFECTION_CHANCE = 0.3f; // 30%感染几率
    private static final int INFECTION_TIME = 200; // 10秒感染时间

    public CorpseGibEntity(EntityType<? extends CorpseGibEntity> type, Level level) {
        super(type, level);
        this.liveTime = 0;
        this.pitchSpin = 0;
        this.yawSpin = 0;
        this.explosion = false;
        this.noCulling = true;
    }

    /**
     * 创建残肢实体 - 参照 EntityGib 构造函数
     */
    public static CorpseGibEntity create(Level level, LivingEntity parent, int gibType, @Nullable Entity explosionSource) {
        CorpseGibEntity gib = new CorpseGibEntity(EntityRegistry.CORPSE_GIB.get(), level);

        gib.parent = parent;
        gib.parentUUID = parent.getUUID();
        gib.setGibType(gibType);
        gib.setParentType(parent.getType().toString());
        gib.setSkeleton(parent.getType().toString().toLowerCase().contains("skeleton"));
        gib.explosion = explosionSource != null;
        gib.setRandomTextureIndex(level.random.nextInt(16));

        // 设置位置和旋转 - 参照 EntityGib 的初始化
        gib.setPos(parent.getX(), parent.getBoundingBox().minY, parent.getZ());
        gib.setYRot(parent.getYRot());
        gib.setXRot(parent.getXRot());

        // 根据类型设置大小和位置偏移
        setupGibByType(gib, parent, gibType);

        // 设置初始速度 (飞溅效果)
        Vec3 parentMotion = parent.getDeltaMovement();
        gib.setDeltaMovement(
                parentMotion.x + (level.random.nextDouble() - level.random.nextDouble()) * 0.25D,
                parentMotion.y + 0.3D,
                parentMotion.z + (level.random.nextDouble() - level.random.nextDouble()) * 0.25D
        );

        // 设置旋转速度
        float i = level.random.nextInt(45) + 5F + level.random.nextFloat();
        float j = level.random.nextInt(45) + 5F + level.random.nextFloat();
        if (level.random.nextInt(2) == 0) i *= -1;
        if (level.random.nextInt(2) == 0) j *= -1;
        
        // 防止 NaN：确保 deltaMovement 的值有效
        double deltaY = gib.getDeltaMovement().y;
        double deltaXZ = Math.sqrt(gib.getDeltaMovement().x * gib.getDeltaMovement().x + gib.getDeltaMovement().z * gib.getDeltaMovement().z);
        gib.pitchSpin = i * (float)(deltaY + 0.3D);
        gib.yawSpin = j * (float)(deltaXZ + 0.3D);

        // 爆炸效果
        if (explosionSource != null) {
            applyExplosionForce(gib, parent, explosionSource);
        }

        return gib;
    }

    /**
     * 根据类型设置残肢属性 - 参照 EntityGib 构造函数
     */
    private static void setupGibByType(CorpseGibEntity gib, LivingEntity parent, int type) {
        double yawRad = Math.toRadians(parent.yBodyRot);

        switch (type) {
            case GIB_TYPE_HEAD -> {
                if (parent.getType().toString().toLowerCase().contains("creeper")) {
                    gib.setPos(gib.getX(), gib.getY() + 1.25D, gib.getZ());
                } else {
                    gib.setPos(gib.getX(), gib.getY() + 1.5D, gib.getZ());
                }
                gib.setYRot(parent.yHeadRot);
            }
            case GIB_TYPE_LEFT_ARM, GIB_TYPE_RIGHT_ARM -> {
                double offset = 0.350D;
                double offset1 = -0.250D;

                if (parent.getType().toString().toLowerCase().contains("skeleton")) {
                    offset -= 0.05D;
                    gib.setPos(gib.getX(), gib.getY() + 0.15D, gib.getZ());
                }
                if (type == GIB_TYPE_RIGHT_ARM) {
                    offset *= -1D;
                }

                double newX = gib.getX() + offset * Math.cos(yawRad) + offset1 * Math.sin(yawRad);
                double newZ = gib.getZ() + offset * Math.sin(yawRad) - offset1 * Math.cos(yawRad);
                gib.setPos(newX, gib.getY() + 1.25D, newZ);
                gib.setXRot(-90F);
            }
            case GIB_TYPE_BODY -> {
                if (parent.getType().toString().toLowerCase().contains("creeper")) {
                    gib.setPos(gib.getX(), gib.getY() + 0.75D, gib.getZ());
                } else {
                    gib.setPos(gib.getX(), gib.getY() + 1.0D, gib.getZ());
                }
            }
            case GIB_TYPE_LEFT_LEG, GIB_TYPE_RIGHT_LEG -> {
                double offset = 0.125D;
                if (type == GIB_TYPE_RIGHT_LEG) {
                    offset *= -1D;
                }
                double newX = gib.getX() + offset * Math.cos(yawRad);
                double newZ = gib.getZ() + offset * Math.sin(yawRad);
                gib.setPos(newX, gib.getY() + 0.375D, newZ);
            }
            default -> { // 苦力怕脚 6+
                double offset = 0.125D;
                double offset1 = -0.250D;

                if (parent.getType().toString().toLowerCase().contains("skeleton")) {
                    offset -= 0.05D;
                    gib.setPos(gib.getX(), gib.getY() + 0.15D, gib.getZ());
                }
                if (type % 2 == 1) {
                    offset *= -1D;
                }
                if (type >= 8) {
                    offset1 *= -1D;
                }

                double newX = gib.getX() + offset * Math.cos(yawRad) + offset1 * Math.sin(yawRad);
                double newZ = gib.getZ() + offset * Math.sin(yawRad) - offset1 * Math.cos(yawRad);
                gib.setPos(newX, gib.getY() + 0.3125D, newZ);
            }
        }
    }

    /**
     * 应用爆炸力量 - 参照 EntityGib 的爆炸处理
     */
    private static void applyExplosionForce(CorpseGibEntity gib, LivingEntity parent, Entity explosionSource) {
        double dist = explosionSource.distanceTo(parent);
        dist = Math.pow(dist / 2D, 2);
        if (dist < 0.1D) dist = 0.1D;

        double mag = 1.0D;
        String sourceType = explosionSource.getType().toString().toLowerCase();

        if (sourceType.contains("tnt")) {
            mag = 1.0D * (4.0 / dist);
        } else if (sourceType.contains("creeper")) {
            mag = 1.0D * (3.0D / dist);
        }

        mag = Math.pow(mag, 2) * 0.2D;
        
        // 防止 mag 变成 NaN 或过大
        if (Double.isNaN(mag) || Double.isInfinite(mag)) {
            mag = 1.0D;
        }
        mag = Math.min(mag, 5.0D); // 限制最大力量
        
        double mag2 = (gib.getY() - explosionSource.getY());

        Vec3 motion = gib.getDeltaMovement();
        gib.setDeltaMovement(
                motion.x * mag,
                mag2 * 0.4D + 0.22D,
                motion.z * mag
        );
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_PARENT_TYPE, "minecraft:zombie");
        builder.define(DATA_IS_SKELETON, false);
        builder.define(DATA_GROUND_TIME, 0);
        builder.define(DATA_INFECTION_PROGRESS, 0.0f);
        builder.define(DATA_RANDOM_TEXTURE_INDEX, -1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setGibType(tag.getInt("GibType"));
        setParentType(tag.getString("ParentType"));
        setSkeleton(tag.getBoolean("IsSkeleton"));
        setGroundTime(tag.getInt("GroundTime"));
        setInfectionProgress(tag.getFloat("InfectionProgress"));
        setRandomTextureIndex(tag.getInt("RandomTextureIndex"));

        if (tag.hasUUID("ParentUUID")) {
            this.parentUUID = tag.getUUID("ParentUUID");
        }

        this.pitchSpin = tag.getFloat("PitchSpin");
        this.yawSpin = tag.getFloat("YawSpin");
        this.liveTime = tag.getInt("LiveTime");
        this.explosion = tag.getBoolean("Explosion");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("GibType", getGibType());
        tag.putString("ParentType", getParentType());
        tag.putBoolean("IsSkeleton", isSkeleton());
        tag.putInt("GroundTime", getGroundTime());
        tag.putFloat("InfectionProgress", getInfectionProgress());
        tag.putInt("RandomTextureIndex", getRandomTextureIndex());

        if (this.parentUUID != null) {
            tag.putUUID("ParentUUID", this.parentUUID);
        }

        tag.putFloat("PitchSpin", this.pitchSpin);
        tag.putFloat("YawSpin", this.yawSpin);
        tag.putInt("LiveTime", this.liveTime);
        tag.putBoolean("Explosion", this.explosion);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }

        // 物理更新
        updatePhysics();

        this.liveTime++;
    }

    /**
     * 客户端更新
     */
    private void clientTick() {
        // 查找父实体用于渲染纹理
        if (this.parent == null && this.parentUUID != null) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.parentUUID);
            if (entity instanceof LivingEntity) {
                this.parent = (LivingEntity) entity;
            }
        }
    }

    /**
     * 服务端更新
     */
    private void serverTick() {
        // 检查生命周期
        if (this.liveTime > MAX_LIFETIME) {
            this.discard();
            return;
        }

        // 身体类型的感染逻辑
        if (getGibType() == GIB_TYPE_BODY) {
            checkAndSpawnInfectedZombie();
        }

        // 更新地面时间
        if (this.onGround()) {
            setGroundTime(getGroundTime() + 1);

            // 落地时间超过最大限制，消失
            if (getGroundTime() > GROUND_TIME_MAX) {
                this.discard();
                return;
            }
        }

        // 生成粒子效果
        if (this.random.nextInt(20) == 0) {
            this.level().addParticle(ParticleTypes.ASH,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    0, 0.02, 0);
        }
    }

    /**
     * 检查感染进度并生成僵尸
     * 由 CorpseInfectionHandler 设置感染进度，这里检查是否完成
     */
    private void checkAndSpawnInfectedZombie() {
        // 只有身体可以被感染
        float progress = getInfectionProgress();

        // 感染完成，生成僵尸
        if (progress >= 1.0f && !this.isRemoved()) {
            spawnZombie();
        }
    }

    /**
     * 增加感染进度（由 CorpseInfectionHandler 调用）
     */
    public void addInfectionProgress(float amount) {
        if (getGibType() == GIB_TYPE_BODY && !this.isRemoved()) {
            float current = getInfectionProgress();
            if (current < 1.0f) {
                setInfectionProgress(Math.min(1.0f, current + amount));
            }
        }
    }

    /**
     * 生成尸兄（模组自定义实体）
     * 支持外部模组通过API注册的感染实体
     */
    private void spawnZombie() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity infectedEntity = null;

        // 1. 尝试通过InfectionAPI创建感染实体
        if (this.parent != null) {
            infectedEntity = InfectionAPI.createInfectedEntity(serverLevel, this.parent);
        }

        // 2. 如果API没有返回，尝试通过实体注册系统创建
        if (infectedEntity == null && this.parent != null) {
            infectedEntity = EntityInfectionRegistry.createInfectedEntity(serverLevel, this.parent);
        }

        // 3. 如果都没有，使用默认的低阶尸兄
        if (infectedEntity == null) {
            var lowerLevelZb = EntityRegistry.LOWER_LEVEL_ZB.get().create(serverLevel);
            if (lowerLevelZb != null) {
                lowerLevelZb.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0);
                infectedEntity = lowerLevelZb;
            }
        }

        if (infectedEntity != null) {
            serverLevel.addFreshEntity(infectedEntity);

            // 播放效果
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    10, 0.3, 0.3, 0.3, 0.01);

            CorpseOrigin.LOGGER.info("尸体感染完成，生成 {} at [{}, {}, {}]",
                    infectedEntity.getType(), this.getX(), this.getY(), this.getZ());

            // 触发感染完成事件
            if (this.parent != null) {
                InfectionAPI.onInfectionComplete(this.parent, infectedEntity);
                NeoForge.EVENT_BUS.post(new InfectionEvent.InfectionCompleteEvent(
                        serverLevel, this.parent, infectedEntity));
            }

            this.discard();
        }
    }

    /**
     * 物理更新 - 参照 EntityGib 的物理逻辑
     */
    private void updatePhysics() {
        // 应用重力
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));
        }

        // 防止 NaN 旋转值
        if (Float.isNaN(this.pitchSpin)) this.pitchSpin = 0;
        if (Float.isNaN(this.yawSpin)) this.yawSpin = 0;

        // 应用旋转
        if (!this.onGround()) {
            float newPitch = this.getXRot() + this.pitchSpin;
            float newYaw = this.getYRot() + this.yawSpin;
            
            // 防止旋转值超出范围
            newPitch = Math.max(-90f, Math.min(90f, newPitch));
            newYaw = newYaw % 360f;
            
            this.setXRot(newPitch);
            this.setYRot(newYaw);
        } else {
            // 落地后减速旋转
            this.pitchSpin *= 0.9f;
            this.yawSpin *= 0.9f;
        }

        // 移动
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        // 摩擦力 - 增加地面摩擦力防止回弹
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0, 0.3));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.98, 0.98));
        }
    }

    /**
     * 被推动时的处理 - 参照 EntityGib 的推动逻辑
     */
    @Override
    public void push(Entity entity) {
        if (!this.level().isClientSide && entity instanceof Mob) {
            // 尸兄 NPC 可能会食用尸体
            if (isCorpseBrotherMob(entity)) {
                tryEatCorpse((Mob) entity);
            }
        }

        // 处理被玩家或其他实体推动时的物理效果
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            // 计算推动方向
            double dx = this.getX() - player.getX();
            double dz = this.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 0.001D) {
                // 归一化方向
                dx /= dist;
                dz /= dist;

                // 根据玩家移动速度计算推动力量
                Vec3 playerMotion = player.getDeltaMovement();
                double pushForce = Math.sqrt(playerMotion.x * playerMotion.x + playerMotion.z * playerMotion.z);

                // 最小推动力
                if (pushForce < 0.1D) {
                    pushForce = 0.1D;
                }

                // 应用推动速度
                this.setDeltaMovement(
                        dx * pushForce * 0.5D,
                        this.getDeltaMovement().y + 0.05D,
                        dz * pushForce * 0.5D
                );
            }
        }

        super.push(entity);
    }

    /**
     * 检查是否是尸兄 NPC
     */
    private boolean isCorpseBrotherMob(Entity entity) {
        // TODO: 实现尸兄 NPC 检测
        return false;
    }

    /**
     * 尝试食用尸体
     */
    private void tryEatCorpse(Mob mob) {
        // TODO: 实现 NPC 食用尸体逻辑
    }

    /**
     * 玩家食用尸体
     */
    public int consume(@Nullable net.minecraft.world.entity.player.Player player) {
        if (this.isRemoved()) return 0;

        int nutrition = switch (getGibType()) {
            case GIB_TYPE_HEAD -> 6;
            case GIB_TYPE_BODY -> 8;
            case GIB_TYPE_LEFT_ARM, GIB_TYPE_RIGHT_ARM -> 4;
            case GIB_TYPE_LEFT_LEG, GIB_TYPE_RIGHT_LEG -> 4;
            default -> 3;
        };

        // 播放效果
        this.level().addParticle(ParticleTypes.ITEM_SLIME,
                this.getX(), this.getY() + 0.3, this.getZ(),
                0, 0.1, 0);

        this.discard();
        return nutrition;
    }

    // Getters and Setters

    public int getGibType() {
        return this.entityData.get(DATA_TYPE);
    }

    public void setGibType(int type) {
        this.entityData.set(DATA_TYPE, type);
    }

    public String getParentType() {
        return this.entityData.get(DATA_PARENT_TYPE);
    }

    public void setParentType(String type) {
        this.entityData.set(DATA_PARENT_TYPE, type);
    }

    public boolean isSkeleton() {
        return this.entityData.get(DATA_IS_SKELETON);
    }

    public void setSkeleton(boolean skeleton) {
        this.entityData.set(DATA_IS_SKELETON, skeleton);
    }

    public int getGroundTime() {
        return this.entityData.get(DATA_GROUND_TIME);
    }

    public void setGroundTime(int time) {
        this.entityData.set(DATA_GROUND_TIME, time);
    }

    public float getInfectionProgress() {
        return this.entityData.get(DATA_INFECTION_PROGRESS);
    }

    public void setInfectionProgress(float progress) {
        this.entityData.set(DATA_INFECTION_PROGRESS, progress);
    }

    public int getRandomTextureIndex() {
        return this.entityData.get(DATA_RANDOM_TEXTURE_INDEX);
    }

    public void setRandomTextureIndex(int index) {
        this.entityData.set(DATA_RANDOM_TEXTURE_INDEX, index);
    }

    @Nullable
    public LivingEntity getParent() {
        return this.parent;
    }

    /**
     * 获取部件类型名称
     */
    public String getPartTypeName() {
        return switch (getGibType()) {
            case GIB_TYPE_HEAD -> "头颅";
            case GIB_TYPE_LEFT_ARM -> "左臂";
            case GIB_TYPE_RIGHT_ARM -> "右臂";
            case GIB_TYPE_BODY -> "躯干";
            case GIB_TYPE_LEFT_LEG -> "左腿";
            case GIB_TYPE_RIGHT_LEG -> "右腿";
            default -> "残肢";
        };
    }

    /**
     * 是否是身体（可被感染）
     */
    public boolean isBody() {
        return getGibType() == GIB_TYPE_BODY;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
