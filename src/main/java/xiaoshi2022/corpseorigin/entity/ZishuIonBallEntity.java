package xiaoshi2022.corpseorigin.entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.ParticleTypes;
import xiaoshi2022.corpseorigin.registry.ModEntities;
public final class ZishuIonBallEntity extends ThrowableItemProjectile {
    public ZishuIonBallEntity(EntityType<? extends ZishuIonBallEntity> t,Level l){super(t,l);setNoGravity(true);}
    public ZishuIonBallEntity(ServerLevel l,ZishuRobotEntity owner,LivingEntity target){
        this(ModEntities.ZISHU_ION_BALL,l);setOwner(owner);setPos(owner.getEyePosition());setDeltaMovement(target.getEyePosition().subtract(position()).normalize().scale(.85));
    }
    @Override protected Item getDefaultItem(){return Items.PRISMARINE_CRYSTALS;}
    @Override public void tick(){super.tick();if(level() instanceof ServerLevel l){l.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),4,.12,.12,.12,.03);if(tickCount>60)discard();}}
    @Override protected boolean canHitEntity(Entity e){return super.canHitEntity(e)&&getOwner() instanceof ZishuRobotEntity owner&&e instanceof LivingEntity living&&owner.canEngage(living);}
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);if(!(level() instanceof ServerLevel l))return;
        if(getOwner() instanceof ZishuRobotEntity owner){
            Vec3 at=hit.getLocation().subtract(getDeltaMovement().normalize().scale(.1));
            for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(2.5))){
                if(!owner.canEngage(target)||target.distanceToSqr(at)>6.25)continue;
                if(l.clip(new ClipContext(at,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
                target.hurtServer(l,damageSources().mobAttack(owner),32);
            }
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,32,.7,.7,.7,.2);
            l.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,net.minecraft.sounds.SoundSource.NEUTRAL,1,1.4f);
        }discard();
    }
}
