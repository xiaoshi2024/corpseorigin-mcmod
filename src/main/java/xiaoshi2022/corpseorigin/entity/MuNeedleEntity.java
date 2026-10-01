package xiaoshi2022.corpseorigin.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModItems;

/**
 * 穆博士投掷的「药剂针头」投射物。
 * <p>
 * 外观<b>复用模组里已有的药剂模型</b>（{@link ModItems#S_AGENT}，GeckoLib 三维药剂瓶），
 * 通过 {@link ThrowableItemProjectile} + 标准 {@code ThrownItemRenderer} 直接渲染物品模型，
 * 无需新贴图。
 * <p>
 * 三种效果，由 {@link #getKind()} 区分：毒针（中毒 II / 8 秒）、虚弱针（虚弱 I + 挖掘疲劳 I / 10 秒）、
 * 迟缓针（缓慢 III / 5 秒）。命中反馈（粒子 + 音效）在 {@link #onHit} 里按类型给。
 * 直线飞行（关闭重力），命中后即消失。
 */
public class MuNeedleEntity extends ThrowableItemProjectile {

    public static final byte KIND_POISON = 0;
    public static final byte KIND_WEAKNESS = 1;
    public static final byte KIND_SLOWNESS = 2;

    private static final float DAMAGE = 4.0F;
    private static final int MAX_LIFE = 80;

    private static final EntityDataAccessor<Byte> DATA_KIND =
            SynchedEntityData.defineId(MuNeedleEntity.class, EntityDataSerializers.BYTE);

    public MuNeedleEntity(EntityType<? extends MuNeedleEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** 从 owner 的眼睛位置发射一枚指定类型的针 */
    public MuNeedleEntity(ServerLevel level, LivingEntity owner, byte kind) {
        this(ModEntities.MU_NEEDLE, level);
        this.setOwner(owner);
        this.setKind(kind);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, KIND_POISON);
    }

    public byte getKind() {
        return this.entityData.get(DATA_KIND);
    }

    public void setKind(byte kind) {
        this.entityData.set(DATA_KIND, kind);
    }

    /** 复用已有药剂模型作为飞行中的外观 */
    @Override
    protected Item getDefaultItem() {
        return ModItems.S_AGENT;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && entity instanceof LivingEntity
                && entity != getOwner()
                && !(entity instanceof MuDoctorEntity);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (this.tickCount > MAX_LIFE) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(this.level() instanceof ServerLevel level)) {
            this.discard();
            return;
        }
        Vec3 at = hit.getLocation();
        LivingEntity victim = hit instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity living ? living : null;
        if (victim != null) {
            applyHit(level, victim, at);
        } else {
            level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 6, 0.15D, 0.15D, 0.15D, 0.02D);
        }
        this.discard();
    }

    private void applyHit(ServerLevel level, LivingEntity victim, Vec3 at) {
        DamageSource src = getOwner() instanceof LivingEntity owner
                ? owner.damageSources().mobAttack(owner)
                : this.damageSources().thrown(this, getOwner());
        victim.hurtServer(level, src, DAMAGE);

        switch (getKind()) {
            case KIND_POISON -> {
                // 中毒 II，8 秒
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1, false, true, true));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        at.x, at.y, at.z, 14, 0.3D, 0.3D, 0.3D, 0.05D);
                level.playSound(null, at.x, at.y, at.z,
                        SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            case KIND_WEAKNESS -> {
                // 虚弱 I + 挖掘疲劳 I，10 秒
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true, true));
                victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 0, false, true, true));
                level.sendParticles(ParticleTypes.SMOKE,
                        at.x, at.y, at.z, 14, 0.3D, 0.3D, 0.3D, 0.02D);
                level.playSound(null, at.x, at.y, at.z,
                        SoundEvents.SLIME_HURT, SoundSource.HOSTILE, 0.9F, 0.5F);
            }
            default -> {
                // 缓慢 III，5 秒
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2, false, true, true));
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        at.x, at.y, at.z, 14, 0.3D, 0.3D, 0.3D, 0.02D);
                level.playSound(null, at.x, at.y, at.z,
                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.9F, 1.4F);
            }
        }
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.2D, 0.2D, 0.2D, 0.2D);
    }
}