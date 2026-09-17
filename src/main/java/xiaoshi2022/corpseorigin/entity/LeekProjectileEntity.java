package xiaoshi2022.corpseorigin.entity;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.List;
import java.util.UUID;

/**
 * 初音尸兄的「投掷大葱」投射物。
 * <p>
 * 原作：初音尸兄在车顶用大葱投掷击杀了怪物控。
 * 飞行逻辑参照 {@link FlyingGreatSwordEntity}（照抄箭矢的直线飞行），带轻微下坠。
 * 渲染用竹节（BAMBOO）代替大葱，直到有正式的大葱贴图/模型。
 */
public class LeekProjectileEntity extends Entity {

    private static final float DAMAGE = 4.0F;
    private static final double SPEED = 1.1D;
    private static final double GRAVITY = 0.015D;
    private static final int MAX_LIFE = 60;
    private static final double HIT_BOX_HALF = 0.4D;

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(LeekProjectileEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(LeekProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(LeekProjectileEntity.class, EntityDataSerializers.FLOAT);

    // ==================== 运行时字段 ====================
    private UUID ownerUUID;
    private Vec3 flyDir = Vec3.ZERO;
    private int lifeTicks = 0;
    private boolean resolved = false;

    public LeekProjectileEntity(EntityType<? extends LeekProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    // ==================== 发射 ====================

    /**
     * 从 thrower 眼睛位置朝目标眼睛方向掷出一根大葱。
     */
    public static void throwLeek(ServerLevel level, Mob thrower, LivingEntity target) {
        LeekProjectileEntity leek = new LeekProjectileEntity(
                ModEntities.LEEK_PROJECTILE, level);

        Vec3 from = thrower.getEyePosition().add(0, -0.1, 0);
        Vec3 dir = target.getEyePosition().subtract(from).normalize();
        leek.ownerUUID = thrower.getUUID();
        leek.flyDir = dir;

        leek.entityData.set(DATA_ITEM, new ItemStack(Items.BAMBOO));
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(dir.y));
        leek.setYRot(yaw);
        leek.setXRot(pitch);
        leek.yRotO = yaw;
        leek.xRotO = pitch;
        leek.entityData.set(DATA_YAW, yaw);
        leek.entityData.set(DATA_PITCH, pitch);

        leek.setPos(from.x + dir.x * 0.6, from.y + dir.y * 0.6, from.z + dir.z * 0.6);
        level.addFreshEntity(leek);

        level.playSound(null, leek.getX(), leek.getY(), leek.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F,
                1.2F + thrower.getRandom().nextFloat() * 0.2F);
    }

    // ==================== Getter（渲染用） ====================

    public ItemStack getItemStack() { return this.entityData.get(DATA_ITEM); }
    public float getSyncedYaw()     { return this.entityData.get(DATA_YAW); }
    public float getSyncedPitch()   { return this.entityData.get(DATA_PITCH); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_YAW, 0f);
        builder.define(DATA_PITCH, 0f);
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;
        if (resolved) return;

        lifeTicks++;

        // 轻微下坠
        flyDir = new Vec3(flyDir.x, flyDir.y - GRAVITY, flyDir.z).normalize();

        // 朝向同步
        float targetYaw = (float) Math.toDegrees(Math.atan2(-flyDir.x, flyDir.z));
        float targetPitch = (float) Math.toDegrees(-Math.asin(flyDir.y));
        this.setYRot(targetYaw);
        this.setXRot(targetPitch);
        this.entityData.set(DATA_YAW, targetYaw);
        this.entityData.set(DATA_PITCH, targetPitch);

        Vec3 step = flyDir.scale(SPEED);
        this.setPos(getX() + step.x, getY() + step.y, getZ() + step.z);

        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.COMPOSTER,
                    getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }

        // 撞方块 → 消失
        if (!this.level().noCollision(this.getBoundingBox().inflate(-0.05))) {
            resolve(false);
            return;
        }

        // 命中生物
        AABB hitBox = this.getBoundingBox().inflate(HIT_BOX_HALF);
        LivingEntity owner = resolveOwner();
        UUID ownerId = ownerUUID;
        List<LivingEntity> hits = level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e.isAlive() && !e.getUUID().equals(ownerId) && !e.isSpectator());

        if (!hits.isEmpty()) {
            LivingEntity target = hits.get(0);
            ServerLevel sl = (ServerLevel) level();

            DamageSource src = owner != null
                    ? owner.damageSources().mobAttack(owner)
                    : this.damageSources().generic();
            target.hurtServer(sl, src, DAMAGE);

            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    6, 0.2, 0.2, 0.2, 0.1);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.8F, 1.2F);

            resolve(true);
            return;
        }

        if (lifeTicks >= MAX_LIFE) {
            resolve(false);
        }
    }

    private LivingEntity resolveOwner() {
        if (ownerUUID == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(ownerUUID) instanceof LivingEntity living ? living : null;
    }

    private void resolve(boolean hit) {
        if (resolved) return;
        resolved = true;

        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME,
                    getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.02);
        }
        this.discard();
    }

    // ==================== NBT ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ownerUUID = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        input.read("FlyDir", Vec3.CODEC).ifPresent(d -> this.flyDir = d);
        this.lifeTicks = input.getIntOr("LifeTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUUID != null) output.store("Owner", UUIDUtil.CODEC, ownerUUID);
        output.store("FlyDir", Vec3.CODEC, flyDir);
        output.putInt("LifeTicks", lifeTicks);
    }
}
