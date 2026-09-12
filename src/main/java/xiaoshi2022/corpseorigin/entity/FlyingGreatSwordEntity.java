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

import java.util.List;
import java.util.UUID;

/**
 * 诗仙剑·第四段：剑意核心实体
 *
 * 生命周期：
 *   1. CHARGE（凝聚）   —— 悬浮在玩家身前 N tick，从 0 放大到 fullScale
 *   2. FLY（飞行）      —— 沿视线飞出，速度 ramp 从 0 到 maxSpeed
 *   3. RESOLVE（终结）  —— 命中或寿命终点，触发 12 格 AABB 挥斩后消散
 */
public class FlyingGreatSwordEntity extends Entity {

    // ==================== 阶段 ====================
    private static final int CHARGE_TICKS = 8;      // 凝聚时长
    private static final int FLY_MAX_LIFE = 60;     // 飞行阶段最长寿命
    private static final double MAX_DIST = 48.0;    // 最大飞行距离
    private static final double MAX_SPEED = 1.4;    // 最大速度 格/tick
    private static final double ACCEL = 0.08;       // 每 tick 加速度

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
    /** 当前缩放（用于渲染，从 0 → fullScale → 飞行时微缩） */
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.FLOAT);
    /** 阶段：0=CHARGE, 1=FLY, 2=RESOLVED */
    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(FlyingGreatSwordEntity.class, EntityDataSerializers.BYTE);

    // ==================== 运行时字段 ====================
    private UUID ownerUUID;
    private int chargeTicks = 0;     // 凝聚已过 tick
    private int flyTicks = 0;        // 飞行已过 tick
    private double travelled = 0.0;
    private Vec3 flyDir = Vec3.ZERO;
    private double currentSpeed = 0.0;
    private boolean resolved = false;

    // ==================== 可配置 ====================
    /** 凝聚完成后剑的最终大小（渲染器以这个为 1.0 基准） */
    private float fullScale = 3.0f;

    public FlyingGreatSwordEntity(EntityType<? extends FlyingGreatSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    /** 发射：进入凝聚阶段 */
    public void launch(Player owner, ItemStack stack) {
        this.ownerUUID = owner.getUUID();
        this.entityData.set(DATA_ITEM, stack.copy());

        Vec3 look = owner.getLookAngle().normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(-Math.asin(look.y));
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
        this.entityData.set(DATA_PHASE, (byte) 0);   // CHARGE
        this.entityData.set(DATA_SCALE, 0.0f);

        this.flyDir = look;

        Vec3 start = owner.position()
                .add(0, owner.getEyeHeight() - 0.2, 0)
                .add(look.scale(1.5));
        this.setPos(start.x, start.y, start.z);
        this.setDeltaMovement(Vec3.ZERO);            // 凝聚阶段不移动
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    /** 外部可调最终缩放（比如武器越强越大） */
    public void setFullScale(float scale) {
        this.fullScale = scale;
    }

    public ItemStack getItemStack() { return this.entityData.get(DATA_ITEM); }
    public float getSyncedYaw()     { return this.entityData.get(DATA_YAW); }
    public float getSyncedPitch()   { return this.entityData.get(DATA_PITCH); }
    public float getRenderScale()   { return this.entityData.get(DATA_SCALE); }
    public byte  getPhase()         { return this.entityData.get(DATA_PHASE); }
    public UUID  getOwnerUUID()     { return this.ownerUUID; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_YAW, 0f);
        builder.define(DATA_PITCH, 0f);
        builder.define(DATA_SCALE, 0f);
        builder.define(DATA_PHASE, (byte) 0);
    }

    private Player resolveOwner() {
        if (ownerUUID == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getPlayerByUUID(ownerUUID);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }
        if (resolved) return;

        byte phase = this.entityData.get(DATA_PHASE);
        if (phase == 0) {
            tickCharge();
        } else if (phase == 1) {
            tickFly();
        }
    }

    // ==================== 阶段 1：凝聚 ====================
    private void tickCharge() {
        chargeTicks++;

        // 缩放 0 → fullScale
        float t = (float) chargeTicks / CHARGE_TICKS;
        t = Math.min(1.0f, t);
        this.entityData.set(DATA_SCALE, fullScale * t);

        // 缓慢跟随玩家视线（让剑"指向"玩家视线方向）
        Player owner = resolveOwner();
        if (owner != null) {
            Vec3 look = owner.getLookAngle().normalize();
            this.flyDir = look;
            float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
            float pitch = (float) Math.toDegrees(-Math.asin(look.y));
            this.entityData.set(DATA_YAW, yaw);
            this.entityData.set(DATA_PITCH, pitch);
            this.setYRot(yaw);
            this.setXRot(pitch);
        }

        // 凝聚粒子：向内收缩的 END_ROD + ENCHANT
        if (level() instanceof ServerLevel sl) {
            int count = 4;
            for (int i = 0; i < count; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5 * (1.0 - t);
                double px = getX() + Math.cos(a) * r;
                double pz = getZ() + Math.sin(a) * r;
                double py = getY() + (Math.random() - 0.5) * 1.5;
                sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0.0);
                sl.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 1, 0, 0, 0, 0.05);
            }
        }

        // 凝聚完成 → 进入飞行
        if (chargeTicks >= CHARGE_TICKS) {
            this.entityData.set(DATA_PHASE, (byte) 1);
            this.currentSpeed = 0.0;

            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
        }
    }

    // ==================== 阶段 2：飞行 ====================
    private void tickFly() {
        flyTicks++;

        // 速度 ramp
        currentSpeed = Math.min(MAX_SPEED, currentSpeed + ACCEL);

        // 移动
        Vec3 step = flyDir.scale(currentSpeed);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        travelled += step.length();

        // 渲染缩放：飞行中略缩，体现"远去"
        float shrink = 1.0f - 0.3f * Math.min(1.0f, (float) flyTicks / FLY_MAX_LIFE);
        this.entityData.set(DATA_SCALE, fullScale * shrink);

        // 轨迹粒子
        spawnTrailParticles();

        // 命中判定
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
        input.read("Item", ItemStack.CODEC).ifPresent(s ->
                this.entityData.set(DATA_ITEM, s));
        // 不保存 phase，加载后重新从 charge 走（可接受）
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUUID != null) {
            output.store("Owner", UUIDUtil.CODEC, ownerUUID);
        }
        output.putInt("ChargeTicks", chargeTicks);
        output.putInt("FlyTicks", flyTicks);
        output.putDouble("Travelled", travelled);
        output.putDouble("Speed", currentSpeed);
        output.store("Item", ItemStack.CODEC, getItemStack());
    }

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

            if (owner != null) {
                owner.swing(InteractionHand.MAIN_HAND, true);
            }

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
                        x, center.y + Math.random() * 2, z,
                        1, 0, 0, 0, 0);
            }

            sl.playSound(null, center.x, center.y, center.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.7F);
        }
        discard();
    }
}