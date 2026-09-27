package xiaoshi2022.corpseorigin.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public class JuQueBeamEntity extends Projectile {

    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(JuQueBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LEVEL =
            SynchedEntityData.defineId(JuQueBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(JuQueBeamEntity.class, EntityDataSerializers.FLOAT);

    private double travelled;
    private int knockbackStrength = 1;
    private int ticksExisted = 0;

    public JuQueBeamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public JuQueBeamEntity(Level level, LivingEntity owner) {
        this(ModEntities.JUQUE_BEAM, level);   // ✅ Fabric 注册引用
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 4.0F);
        builder.define(LEVEL, 1);
        builder.define(POWER, 0F);
    }

    public JuQueBeamEntity setLevel(int level) {
        this.getEntityData().set(LEVEL, Math.clamp(level, 1, 20));
        return this;
    }

    @Override public void setOwner(Entity owner) {
        super.setOwner(owner);
        if (!level().isClientSide() && owner instanceof LivingEntity living)
            entityData.set(POWER, powerFor(living));
    }

    /** Snapshot the firing body's own progression and current attack, including equipment and buffs. */
    public static float powerFor(LivingEntity owner) {
        int evolution=1;
        if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
            evolution=xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(
                    xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
        } else if (owner instanceof CloneAvatarEntity clone && clone.getBodyState()!=null) {
            var character=clone.getBodyState().getComponent().as(xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent.class);
            if(character!=null)evolution=character.getEvolutionLevel();
        }
        var attack=owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double bonus=attack==null?0:Math.clamp((attack.getValue()-4)/80.0,0,.25);
        return (float)Math.clamp((evolution-1)/19.0+bonus,0,1);
    }

    public float getPower() { return entityData.get(POWER); }

    public static double rangeFor(int techniqueLevel, float power) {
        return 1.5*(12+Math.clamp(techniqueLevel,1,20))*(1+2*Math.clamp(power,0,1));
    }

    public double getMaxRange() { return rangeFor(entityData.get(LEVEL),getPower()); }

    public JuQueBeamEntity setDamage(float amount) {
        this.getEntityData().set(DAMAGE, amount);
        return this;
    }

    public float getVelocity() {
        return 1.5F;
    }

    @Override
    public void tick() {
        super.tick();

        double remaining=getMaxRange()-travelled;
        if (++ticksExisted > Math.ceil(getMaxRange()/.5)+20 || remaining<=1.0E-6) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 pos = this.position();
        if(motion.length()>remaining) {
            motion=motion.normalize().scale(remaining);
            this.setDeltaMovement(motion);
        }
        if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(pos.add(motion)))) {
            this.discard();
            return;
        }

        // 气只走服务端→客户端：本实体在服务端也 tick，直接发一团气（客户端不再自己撒粒子）。
        // The synchronized projectile renders the qi volume itself; spherical cloud bursts
        // would obscure its crescent and leave an unrelated red cloud behind the player's sword.

        // 移动和碰撞检测
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            this.discard();
            return;
        }

        this.setPos(pos.add(motion));
        travelled+=motion.length();

        if (this.horizontalCollision || this.verticalCollision) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && (!(getOwner() instanceof xiaoshi2022.corpseorigin.entity.CloneAvatarEntity clone)
                || !(entity instanceof LivingEntity target) || xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.canHit(clone, target));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        if (!this.level().isClientSide() && owner instanceof LivingEntity livingOwner) {
            if (target instanceof LivingEntity livingTarget && target != owner) {
                float damage = this.getEntityData().get(DAMAGE);
                DamageSource damageSource = this.damageSources().indirectMagic(this, livingOwner);

                livingTarget.hurt(damageSource, damage);
                if (this.knockbackStrength > 0) {
                        Vec3 knockbackVec = this.getDeltaMovement().normalize()
                                .scale(this.knockbackStrength * 0.6);
                        livingTarget.push(knockbackVec.x, 0.1, knockbackVec.z);
                    }

                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 0.5F);

            }
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.3F, 1.0F);
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setDamage(input.getFloatOr("Damage", 4.0F));
        this.setLevel(input.getIntOr("Level", 1));
        this.ticksExisted = input.getIntOr("TicksExisted", 0);
        float power=input.getFloatOr("Power",0);
        entityData.set(POWER,Float.isFinite(power)?Math.clamp(power,0,1):0);
        double distance=input.getDoubleOr("Travelled",0);
        travelled=Double.isFinite(distance)?Math.clamp(distance,0,getMaxRange()):0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", this.getEntityData().get(DAMAGE));
        output.putInt("Level", this.getEntityData().get(LEVEL));
        output.putInt("TicksExisted", this.ticksExisted);
        output.putFloat("Power",getPower());
        output.putDouble("Travelled",travelled);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
