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
    private static final EntityDataAccessor<Float> SLASH_ROLL =
            SynchedEntityData.defineId(JuQueBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> REALM_TIER = SynchedEntityData.defineId(JuQueBeamEntity.class, EntityDataSerializers.INT);
    /** 施法者角色气息色（0xRRGGBB），渲染端据此给刀身/光束染色 */
    private static final EntityDataAccessor<Integer> AURA = SynchedEntityData.defineId(JuQueBeamEntity.class, EntityDataSerializers.INT);
    private final java.util.Set<java.util.UUID> hitTargets=new java.util.HashSet<>();
    public int getRealmTier(){return entityData.get(REALM_TIER);}
    public float getBladeHeight(){return xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.height(getRealmTier());}
    public float getBladeDepth(){return xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.depth(getRealmTier());}
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
        builder.define(REALM_TIER,1);
        builder.define(DAMAGE, 4.0F);
        builder.define(LEVEL, 1);
        builder.define(POWER, 0F);
        builder.define(SLASH_ROLL, -25F);
        builder.define(AURA, xiaoshi2022.corpseorigin.character.CharacterAuraColors.DEFAULT);
    }

    public JuQueBeamEntity setLevel(int level) {
        this.getEntityData().set(LEVEL, Math.clamp(level, 1, 20));
        return this;
    }

    @Override public void setOwner(Entity owner) {
        super.setOwner(owner);
        if (!level().isClientSide() && owner instanceof LivingEntity living) {
            entityData.set(REALM_TIER,Math.clamp(combatLevel(living),1,20));
            entityData.set(POWER, powerFor(living));
            entityData.set(SLASH_ROLL,(random.nextBoolean()?1:-1)*(12+random.nextFloat()*33));
            entityData.set(AURA, auraOf(living));
        }
    }

    /** 施法者角色气息色：玩家取角色表，傀儡取其形体角色，其余回落默认剑罡金 */
    public static int auraOf(LivingEntity owner) {
        if (owner instanceof net.minecraft.server.level.ServerPlayer player)
            return xiaoshi2022.corpseorigin.character.CharacterAuraColors.aura(
                    xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(player));
        if (owner instanceof CloneAvatarEntity clone && clone.getBodyState()!=null) {
            var character=clone.getBodyState().getComponent().as(xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent.class);
            if(character!=null) return xiaoshi2022.corpseorigin.character.CharacterAuraColors.aura(character.getCharacterId());
        }
        return xiaoshi2022.corpseorigin.character.CharacterAuraColors.DEFAULT;
    }

    public int getAura() { return entityData.get(AURA); }

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
    public float getSlashRoll() { return entityData.get(SLASH_ROLL); }

    private static int combatLevel(LivingEntity entity) {
        if(entity instanceof net.minecraft.server.level.ServerPlayer player)
            return xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(
                    xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
        if(entity instanceof CloneAvatarEntity clone){
            var body=clone.getBodyState();
            var character=body==null?null:body.getComponent().as(xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent.class);
            return character==null?1:character.getEvolutionLevel();
        }
        return 0;
    }

    protected float damageFor(LivingEntity owner, LivingEntity target) {
        return xiaoshi2022.corpseorigin.growth.BalanceRules.swordQiDamage(
                entityData.get(DAMAGE),target.getMaxHealth(),combatLevel(owner),combatLevel(target));
    }

    public static double rangeFor(int techniqueLevel, float power) {
        return 1.5*(12+Math.clamp(techniqueLevel,1,20))*(2+10*Math.clamp(power,0,1));
    }

    public static float openingScale(float power) { return 2+10*Math.clamp(power,0,1); }
    public static float bladeScale(float power) { return 2+4*Math.clamp(power,0,1); }

    /** Shared visual/collision profile, including the rounded ridge and pointed tips. */
    public static float crescentX(float height,float across) {
        float t=Math.min(1,Math.abs(height)),rounding=.42F;
        float sweep=((float)Math.sqrt(t*t+rounding*rounding)-rounding)
                /((float)Math.sqrt(1+rounding*rounding)-rounding);
        float outer=1.2F*(1.08F*(1-sweep)+.10F*sweep*(1-sweep)-.55F);
        float inner=1.2F*(-.55F+.43F*(1-t*t));
        return inner+(outer-inner)*across;
    }

    private HitResult findQiHit(Vec3 start,Vec3 motion) {
        HitResult nearest=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        Vec3 end=nearest.getType()==HitResult.Type.BLOCK?nearest.getLocation():start.add(motion);
        double best=nearest.getType()==HitResult.Type.MISS?Double.POSITIVE_INFINITY:start.distanceToSqr(nearest.getLocation());
        Vec3 direction=motion.lengthSqr()>1.0E-8?motion.normalize():getLookAngle();
        var rotation=new org.joml.Quaternionf().rotationY((float)Math.atan2(direction.x,direction.z))
                .rotateX((float)-Math.asin(Math.clamp(direction.y,-1,1)))
                .rotateZ((float)Math.toRadians(getSlashRoll())).rotateY((float)Math.PI/2);
        float width=getBladeDepth(),height=getBladeHeight()/2.5f;
        var tip=rotation.transform(new org.joml.Vector3f(0,1.25f*height,0));
        var extent=new Vec3(Math.abs(tip.x)+width,Math.abs(tip.y)+width,Math.abs(tip.z)+width);
        var candidates=level().getEntities(this,new net.minecraft.world.phys.AABB(start.subtract(extent),start.add(extent)).expandTowards(motion),
                e->e instanceof LivingEntity&&canHitEntity(e));
        int candidateBudget=128;
        // Sweep overlapping sections of the actual crescent, rather than a huge enclosing cube.
        // Motion sweeps prevent fast beams from jumping over targets between ticks.
        for(var target:candidates){
            if(candidateBudget--<=0)break;
            var visible=level().clip(new net.minecraft.world.level.ClipContext(start,target.getBoundingBox().getCenter(),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
            if(visible.getType()!=HitResult.Type.MISS)continue;
            int sections=Math.clamp((int)getBladeHeight(),32,320);
            for(int section=0;section<=sections;section++){
                float h=-1+2f*section/sections;
                var local=rotation.transform(new org.joml.Vector3f(crescentX(h,.5F)*width,h*1.25F*height,0));
                var offset=new Vec3(local.x,local.y,local.z);
                double radius=Math.max(.55,getBladeHeight()/sections*.55)+(crescentX(h,1)-crescentX(h,0))*width*.5;
                var bounds=target.getBoundingBox().inflate(radius);
                Vec3 from=start.add(offset),to=end.add(offset);
                var hit=bounds.contains(from)?java.util.Optional.of(from):bounds.clip(from,to);
                if(hit.isEmpty())continue;
                double distance=from.distanceToSqr(hit.get());
                if(distance<best){best=distance;nearest=new EntityHitResult(target,hit.get());}
            }
        }
        return nearest;
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double visibleRange=getMaxRange()+32;
        return distance<visibleRange*visibleRange;
    }

    public double getMaxRange() { return Math.max(rangeFor(entityData.get(LEVEL),getPower()),xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.range(getRealmTier())); }

    public JuQueBeamEntity setDamage(float amount) {
        this.getEntityData().set(DAMAGE, amount);
        return this;
    }

    public float getVelocity() {
        return 1.5F + Math.max(0,getRealmTier()-9)*.45f;
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
        // Only the server decides impacts. A client's owner/tracking data can arrive after the projectile.
        if(!level().isClientSide()) {
            HitResult hitResult = findQiHit(pos,motion);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(hitResult);
                if(isRemoved())return;
                if(hitResult.getType()==HitResult.Type.BLOCK || getRealmTier()<10){this.discard();return;}
                if(hitTargets.size()>=64){this.discard();return;}
            }
        }

        this.setPos(pos.add(motion));
        travelled+=motion.length();

        if (this.horizontalCollision || this.verticalCollision) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if(hitTargets.contains(entity.getUUID()))return false;
        Entity owner=getOwner();
        // Vanilla's leftOwner guard expires once the tiny projectile center leaves the caster.
        // The growing crescent can still overlap them, so exclude the caster for the entire flight.
        if(entity==owner || owner!=null && owner.isPassengerOfSameVehicle(entity))return false;
        if(owner!=null && entity instanceof OwnerBound owned && owned.isOwnedBy(owner))return false;
        if(owner instanceof LivingEntity caster && entity instanceof LivingEntity target
                && !xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.canHit(caster,target))return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        if (!this.level().isClientSide() && owner instanceof LivingEntity livingOwner) {
            if (target instanceof LivingEntity livingTarget && target != owner) {
                float damage = damageFor(livingOwner,livingTarget);
                DamageSource damageSource = this.damageSources().mobProjectile(this, livingOwner);

                boolean hit=livingTarget.hurtServer((net.minecraft.server.level.ServerLevel)level(),damageSource,damage);
                hitTargets.add(livingTarget.getUUID());
                if(hit)xiaoshi2022.corpseorigin.skill.chapter.SwordImpact.send((net.minecraft.server.level.ServerLevel)level(),
                        livingTarget.getBoundingBox().getCenter(),getDeltaMovement(),getRealmTier(),0,livingTarget.getId(),owner.getId());
                if (this.knockbackStrength > 0) {
                        Vec3 knockbackVec = this.getDeltaMovement().normalize()
                                .scale(this.knockbackStrength * 0.6);
                        livingTarget.push(knockbackVec.x, 0.1, knockbackVec.z);
                    }

                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 0.5F);

            }
            if(getRealmTier()<10)this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (!this.level().isClientSide()) {
            var server=(net.minecraft.server.level.ServerLevel)level();
            boolean rift=getOwner() instanceof net.minecraft.server.level.ServerPlayer p
                    && xiaoshi2022.corpseorigin.skill.chapter.SwordRift.start(p,result.getLocation(),getDeltaMovement(),getRealmTier());
            if(!rift)xiaoshi2022.corpseorigin.skill.chapter.SwordImpact.send(server,result.getLocation(),getDeltaMovement(),getRealmTier(),0,-1,getOwner()==null?-1:getOwner().getId());
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.3F, 1.0F);
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(REALM_TIER,Math.clamp(input.getIntOr("RealmTier",1),1,20));
        this.setDamage(input.getFloatOr("Damage", 4.0F));
        this.setLevel(input.getIntOr("Level", 1));
        this.ticksExisted = input.getIntOr("TicksExisted", 0);
        float power=input.getFloatOr("Power",0);
        entityData.set(POWER,Float.isFinite(power)?Math.clamp(power,0,1):0);
        float roll=input.getFloatOr("SlashRoll",-25);
        entityData.set(SLASH_ROLL,Float.isFinite(roll)?Math.clamp(roll,-45,45):-25);
        double distance=input.getDoubleOr("Travelled",0);
        travelled=Double.isFinite(distance)?Math.clamp(distance,0,getMaxRange()):0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("RealmTier",getRealmTier());
        output.putFloat("Damage", this.getEntityData().get(DAMAGE));
        output.putInt("Level", this.getEntityData().get(LEVEL));
        output.putInt("TicksExisted", this.ticksExisted);
        output.putFloat("Power",getPower());
        output.putFloat("SlashRoll",getSlashRoll());
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
