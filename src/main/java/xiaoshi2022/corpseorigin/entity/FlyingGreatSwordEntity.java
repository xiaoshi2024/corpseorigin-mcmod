package xiaoshi2022.corpseorigin.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.List;
import java.util.UUID;

/**
 * 诗仙剑·剑意核心实体
 *
 * 飞行行为完全照抄箭矢（AbstractArrow）：
 *   - launch() 用 owner 视线初始化 yRot/xRot
 *   - 每 tick 根据 flyDir 算目标角度，再 lerpRotation 平滑靠拢 20%
 *   - 位移沿 flyDir 直线，不落地
 */
public class FlyingGreatSwordEntity extends Entity {

    // ==================== 阶段 ====================
    private static final int CHARGE_TICKS = 2;
    private static final int FLY_MAX_LIFE = 60;
    private static final double MAX_DIST = 48.0;
    private static final double MAX_SPEED = 1.4;

    // ==================== 挥斩 ====================
    private static final double HIT_BOX_SIZE = 1.5;
    private static final double SLASH_RADIUS = 12.0;
    private static final float DIRECT_DAMAGE = 666.0F;
    private static final float SLASH_DAMAGE = 20.0F;

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MODEL_YAW_OFFSET =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MODEL_PITCH_OFFSET =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);

    // ==================== 运行时字段 ====================
    private UUID ownerUUID;
    private int chargeTicks = 0;
    private int flyTicks = 0;
    private double travelled = 0.0;
    private Vec3 flyDir = Vec3.ZERO;
    private double currentSpeed = 0.0;
    private boolean resolved = false;

    private float fullScale = 3.0f;
    private float spinDegPerTick = 0f;

    private DirectionMode directionMode = DirectionMode.FIXED;
    private LivingEntity homingTarget;
    private Vec3 targetPoint;
    private double turnSpeedDeg = 6.0;
    private boolean steerDuringCharge = false;
    private boolean steerDuringFly = false;

    public enum DirectionMode {
        FIXED,
        FOLLOW_OWNER,
        HOME_TARGET,
        TO_POINT
    }

    public FlyingGreatSwordEntity(EntityType<? extends FlyingGreatSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    // ==================== ✅ 统一发射入口 ====================

    /**
     * 创建一把"剑尖朝向飞行方向（= 玩家视线方向）"的大剑实体，并加入世界。
     * 所有发射大剑的地方都调用这个方法。
     *
     * @param level 服务端世界
     * @param owner 发射者
     * @param stack 用于渲染的物品
     * @param scale 整体缩放
     * @param spin  自旋角度/tick（0 = 不自旋）
     */
    public static FlyingGreatSwordEntity spawnDirected(ServerLevel level, Player owner,
                                                       ItemStack stack, float scale, float spin) {
        FlyingGreatSwordEntity sword = new FlyingGreatSwordEntity(
                ModEntities.FLYING_GREAT_SWORD, level);

        // 按物品类型取模型修正偏移
        float[] fix = modelFixFor(stack);   // {roll, yawOffset, pitchOffset}

        sword.setFullScale(scale)
                .setSpin(spin)
                .setDirectionMode(DirectionMode.FIXED)
                .setSteerDuringCharge(false)
                .setSteerDuringFly(false)
                .setRenderRotation(fix[0], fix[1], fix[2]);
        sword.launch(owner, stack);
        level.addFreshEntity(sword);
        return sword;
    }

    /**
     * 返回模型修正 {roll, yawOffset, pitchOffset}。
     * 默认（普通剑类）：{0, 0, 0} —— 渲染器里的 -90° X 已把剑尖掰向前方。
     * 巨阙走 Geo item 渲染，朝向不同，单独给偏移。
     */
    private static float[] modelFixFor(ItemStack stack) {
        // 巨阙：Geo 模型剑尖朝 +Y，根骨骼自带 [0,-90,0]，需要额外 yaw 抵消
        if (stack.getItem() instanceof xiaoshi2022.corpseorigin.item.sword.JuQue) {
            return new float[]{ 0f, 90f, 0f };
        }
        // 未来其他特殊物品在这里加分支
        // if (stack.getItem() instanceof Xxx) return new float[]{ ... };

        // 普通物品（铁剑/钻石剑等，模型剑尖朝 +Y）
        return new float[]{ 0f, 0f, 0f };
    }

    // ==================== launch ====================

    public void launch(Player owner, ItemStack stack) {
        this.ownerUUID = owner.getUUID();
        this.entityData.set(DATA_ITEM, stack.copy());

        // ✅ 发射瞬间锁定方向 = 玩家视线方向（之后不再改变，像箭矢一样直射）
        Vec3 look = owner.getLookAngle().normalize();
        this.flyDir = look;

        // ✅ 像箭矢一样：从飞行方向直接计算 yaw/pitch，确保剑尖朝向 = 飞行方向
        //    （不能用 owner.getYRot()，那是身体朝向，可能和视线方向有偏差，导致开局"扭一下"）
        float initYaw   = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float initPitch = (float) Math.toDegrees(-Math.asin(look.y));
        this.setYRot(initYaw);
        this.setXRot(initPitch);
        this.yRotO = initYaw;
        this.xRotO = initPitch;
        this.entityData.set(DATA_YAW, initYaw);
        this.entityData.set(DATA_PITCH, initPitch);

        this.entityData.set(DATA_PHASE, (byte) 0);
        this.entityData.set(DATA_SCALE, 0.0f);

        Vec3 start = owner.position()
                .add(0, owner.getEyeHeight() - 0.2, 0)
                .add(look.scale(1.5));
        this.setPos(start.x, start.y, start.z);
        this.setDeltaMovement(Vec3.ZERO);

        this.currentSpeed = MAX_SPEED;
    }

    // ==================== Setter ====================

    public FlyingGreatSwordEntity setFullScale(float scale) {
        this.fullScale = scale;
        return this;
    }

    public FlyingGreatSwordEntity setSpin(float degPerTick) {
        this.spinDegPerTick = degPerTick;
        return this;
    }

    public FlyingGreatSwordEntity setRenderRotation(float roll, float yawOffset, float pitchOffset) {
        this.entityData.set(DATA_ROLL, roll);
        this.entityData.set(DATA_MODEL_YAW_OFFSET, yawOffset);
        this.entityData.set(DATA_MODEL_PITCH_OFFSET, pitchOffset);
        return this;
    }

    public FlyingGreatSwordEntity setDirectionMode(DirectionMode mode) {
        this.directionMode = mode;
        return this;
    }

    public FlyingGreatSwordEntity setTurnSpeed(double degPerTick) {
        this.turnSpeedDeg = degPerTick;
        return this;
    }

    public FlyingGreatSwordEntity setSteerDuringCharge(boolean v) {
        this.steerDuringCharge = v;
        return this;
    }

    public FlyingGreatSwordEntity setSteerDuringFly(boolean v) {
        this.steerDuringFly = v;
        return this;
    }

    public FlyingGreatSwordEntity setTargetPoint(Vec3 point) {
        this.targetPoint = point;
        this.directionMode = DirectionMode.TO_POINT;
        return this;
    }

    public FlyingGreatSwordEntity setHomingTarget(LivingEntity target) {
        this.homingTarget = target;
        this.directionMode = DirectionMode.HOME_TARGET;
        return this;
    }

    // ==================== Getter ====================

    public ItemStack getItemStack()        { return this.entityData.get(DATA_ITEM); }
    public float getSyncedYaw()            { return this.entityData.get(DATA_YAW); }
    public float getSyncedPitch()          { return this.entityData.get(DATA_PITCH); }
    public float getRenderScale()          { return this.entityData.get(DATA_SCALE); }
    public byte  getPhase()                { return this.entityData.get(DATA_PHASE); }
    public UUID  getOwnerUUID()            { return this.ownerUUID; }
    public float getRenderRoll()           { return this.entityData.get(DATA_ROLL); }
    public float getModelYawOffset()       { return this.entityData.get(DATA_MODEL_YAW_OFFSET); }
    public float getModelPitchOffset()     { return this.entityData.get(DATA_MODEL_PITCH_OFFSET); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_YAW, 0f);
        builder.define(DATA_PITCH, 0f);
        builder.define(DATA_SCALE, 0f);
        builder.define(DATA_PHASE, (byte) 0);
        builder.define(DATA_ROLL, 0f);
        builder.define(DATA_MODEL_YAW_OFFSET, 0f);
        builder.define(DATA_MODEL_PITCH_OFFSET, 0f);
    }

    private Player resolveOwner() {
        if (ownerUUID == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getPlayerByUUID(ownerUUID);
    }

    // ==================== ✅ 照抄 AbstractArrow.lerpRotation ====================

    /**
     * 每次只向目标角度靠拢 20%，避免突然转向造成的抖动。
     * 与 AbstractArrow.lerpRotation 完全一致。
     */
    protected static float lerpRotation(float current, float target) {
        while (target - current < -180.0F) target += 360.0F;
        while (target - current >= 180.0F) target -= 360.0F;
        return current + (target - current) * 0.2F;
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;
        if (resolved) return;

        byte phase = this.entityData.get(DATA_PHASE);
        if (phase == 0) tickCharge();
        else if (phase == 1) tickFly();
    }

    private void tickCharge() {
        chargeTicks++;

        float t = Math.min(1.0f, (float) chargeTicks / CHARGE_TICKS);
        this.entityData.set(DATA_SCALE, fullScale * t);

        if (steerDuringCharge) {
            updateFlyDirection();
        }
        // ✅ lerp 版本，加回来
        syncRotationFromDir();

        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5 * (1.0 - t);
                double px = getX() + Math.cos(a) * r;
                double pz = getZ() + Math.sin(a) * r;
                double py = getY() + (Math.random() - 0.5) * 1.5;
                sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0.0);
                sl.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 1, 0, 0, 0, 0.05);
            }
        }

        if (chargeTicks >= CHARGE_TICKS) {
            this.entityData.set(DATA_PHASE, (byte) 1);
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
        }
    }

    private void tickFly() {
        flyTicks++;

        if (steerDuringFly) {
            updateFlyDirection();
        }

        if (spinDegPerTick != 0f) {
            float currentRoll = this.entityData.get(DATA_ROLL);
            this.entityData.set(DATA_ROLL, currentRoll + spinDegPerTick);
        }

        Vec3 step = flyDir.scale(currentSpeed);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        travelled += step.length();

        // ✅ lerp 版本，加回来
        syncRotationFromDir();

        float shrink = 1.0f - 0.3f * Math.min(1.0f, (float) flyTicks / FLY_MAX_LIFE);
        this.entityData.set(DATA_SCALE, fullScale * shrink);

        spawnTrailParticles();

        AABB hitBox = new AABB(
                getX() - HIT_BOX_SIZE, getY() - HIT_BOX_SIZE, getZ() - HIT_BOX_SIZE,
                getX() + HIT_BOX_SIZE, getY() + HIT_BOX_SIZE, getZ() + HIT_BOX_SIZE);

        Player owner = resolveOwner();
        UUID ownerId = owner != null ? owner.getUUID() : null;

        List<LivingEntity> hits = level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e.isAlive() && (ownerId == null || !e.getUUID().equals(ownerId)));

        if (!hits.isEmpty()) {
            ServerLevel sl = (ServerLevel) level();
            DamageSource src = owner != null
                    ? owner.damageSources().playerAttack(owner)
                    : damageSources().generic();
            for (LivingEntity t : hits) {
                t.hurtServer(sl, src, DIRECT_DAMAGE);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
            }
            sl.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.8F);
            resolveFinalSlash();
            return;
        }

        if (flyTicks >= FLY_MAX_LIFE || travelled >= MAX_DIST) {
            resolveFinalSlash();
        }
    }

    // ==================== 方向更新 ====================

    private void updateFlyDirection() {
        Player owner = resolveOwner();
        Vec3 desired = null;

        switch (directionMode) {
            case FIXED -> { return; }
            case FOLLOW_OWNER -> {
                if (owner != null) desired = owner.getLookAngle().normalize();
            }
            case HOME_TARGET -> {
                if (homingTarget != null && homingTarget.isAlive()) {
                    Vec3 toTarget = homingTarget.getEyePosition().subtract(this.getEyePosition());
                    if (toTarget.lengthSqr() > 1.0E-6) desired = toTarget.normalize();
                } else if (owner != null) {
                    desired = owner.getLookAngle().normalize();
                }
            }
            case TO_POINT -> {
                if (targetPoint != null) {
                    Vec3 toPoint = targetPoint.subtract(this.position());
                    if (toPoint.lengthSqr() > 1.0E-6) desired = toPoint.normalize();
                }
            }
        }

        if (desired == null) return;

        double maxRad = Math.toRadians(turnSpeedDeg);
        double dot = Math.max(-1.0, Math.min(1.0, flyDir.dot(desired)));
        double angle = Math.acos(dot);

        if (angle <= maxRad || angle < 1.0E-6) {
            flyDir = desired;
        } else {
            Vec3 axis = flyDir.cross(desired);
            if (axis.lengthSqr() < 1.0E-8) {
                axis = flyDir.cross(new Vec3(0, 1, 0));
                if (axis.lengthSqr() < 1.0E-8) axis = flyDir.cross(new Vec3(1, 0, 0));
            }
            axis = axis.normalize();
            double t = maxRad / angle;
            flyDir = rotateAroundAxis(flyDir, axis, angle * t).normalize();
        }

        // 转向模式下，朝向也走 lerp（syncRotationFromDir 内部就是 lerp）
        syncRotationFromDir();
    }

    /**
     * ✅ 照抄 AbstractArrow.tick 里的朝向逻辑：
     *    根据 flyDir 算出目标 yaw/pitch，再 lerpRotation 平滑靠拢 20%。
     */
    private void syncRotationFromDir() {
        float targetYaw   = (float) Math.toDegrees(Math.atan2(-flyDir.x, flyDir.z));
        float targetPitch = (float) Math.toDegrees(-Math.asin(flyDir.y));

        float newYaw   = lerpRotation(this.getYRot(),   targetYaw);
        float newPitch = lerpRotation(this.getXRot(), targetPitch);

        this.setYRot(newYaw);
        this.setXRot(newPitch);
        this.entityData.set(DATA_YAW, newYaw);
        this.entityData.set(DATA_PITCH, newPitch);
    }

    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = v.dot(axis);
        Vec3 cross = axis.cross(v);
        return new Vec3(
                v.x * cos + cross.x * sin + axis.x * dot * (1 - cos),
                v.y * cos + cross.y * sin + axis.y * dot * (1 - cos),
                v.z * cos + cross.z * sin + axis.z * dot * (1 - cos)
        );
    }

    // ==================== 伤害 / NBT ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ownerUUID = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.chargeTicks = input.getIntOr("ChargeTicks", 0);
        this.flyTicks = input.getIntOr("FlyTicks", 0);
        this.travelled = input.getDoubleOr("Travelled", 0.0);
        this.currentSpeed = input.getDoubleOr("Speed", 0.0);
        this.fullScale = input.getFloatOr("FullScale", 3.0f);
        this.spinDegPerTick = input.getFloatOr("Spin", 0f);
        input.read("Item", ItemStack.CODEC).ifPresent(s ->
                this.entityData.set(DATA_ITEM, s));

        input.getString("DirMode").ifPresent(s -> {
            try { this.directionMode = DirectionMode.valueOf(s); } catch (Exception ignored) {}
        });
        this.turnSpeedDeg = input.getDoubleOr("TurnSpeed", 6.0);
        this.steerDuringCharge = input.getBooleanOr("SteerCharge", false);
        this.steerDuringFly = input.getBooleanOr("SteerFly", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUUID != null) output.store("Owner", UUIDUtil.CODEC, ownerUUID);
        output.putInt("ChargeTicks", chargeTicks);
        output.putInt("FlyTicks", flyTicks);
        output.putDouble("Travelled", travelled);
        output.putDouble("Speed", currentSpeed);
        output.putFloat("FullScale", fullScale);
        output.putFloat("Spin", spinDegPerTick);
        output.store("Item", ItemStack.CODEC, getItemStack());
        output.putString("DirMode", directionMode.name());
        output.putDouble("TurnSpeed", turnSpeedDeg);
        output.putBoolean("SteerCharge", steerDuringCharge);
        output.putBoolean("SteerFly", steerDuringFly);
    }

    // ==================== 粒子 & 终结 ====================

    private void spawnTrailParticles() {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(), getZ(), 3, 0.3, 0.3, 0.3, 0.0);
        sl.sendParticles(ParticleTypes.CRIT,          getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.1);
        sl.sendParticles(ParticleTypes.END_ROD,       getX(), getY(), getZ(), 2, 0.1, 0.1, 0.1, 0.02);
    }

    private void resolveFinalSlash() {
        if (resolved) return;
        resolved = true;
        this.entityData.set(DATA_PHASE, (byte) 2);

        if (level() instanceof ServerLevel sl) {
            Player owner = resolveOwner();
            if (owner != null) owner.swing(InteractionHand.MAIN_HAND, true);

            Vec3 center = position();
            AABB slashBox = new AABB(
                    center.x - SLASH_RADIUS, center.y - SLASH_RADIUS, center.z - SLASH_RADIUS,
                    center.x + SLASH_RADIUS, center.y + SLASH_RADIUS, center.z + SLASH_RADIUS);

            UUID ownerId = owner != null ? owner.getUUID() : null;
            List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, slashBox,
                    e -> e.isAlive() && (ownerId == null || !e.getUUID().equals(ownerId)));

            DamageSource src = owner != null
                    ? owner.damageSources().playerAttack(owner)
                    : damageSources().generic();

            for (LivingEntity t : targets) {
                t.hurtServer(sl, src, SLASH_DAMAGE);
            }

            for (int i = 0; i < 180; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * SLASH_RADIUS;
                double x = center.x + Math.cos(a) * r;
                double z = center.z + Math.sin(a) * r;
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        x, center.y + Math.random() * 2, z, 1, 0, 0, 0, 0);
            }

            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.7F);
        }
        discard();
    }
}