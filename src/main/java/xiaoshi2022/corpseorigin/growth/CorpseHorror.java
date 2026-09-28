package xiaoshi2022.corpseorigin.growth;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.entity.CorpseMaggotEntity;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public final class CorpseHorror {
    private CorpseHorror(){}
    public static CorpseHorrorConfig config(){return CorpseConfig.get().corpseHorror;}
    public static boolean applies(LowerLevelZbEntity z){
        return z.getType()==ModEntities.LOWER_LEVEL_ZB && (z.level().isClientSide()?z.isHorrorActive():config().enabled);
    }
    public static boolean validPrey(LivingEntity attacker,LivingEntity target){
        return target!=null && target.isAlive() && target!=attacker && !target.isSpectator()
                && !(target instanceof Player p && p.isCreative()) && target.getBbWidth()<2.5
                && ZombieKin.canAttack(attacker,target) && !attacker.isAlliedTo(target);
    }
    /** Stable independent rolls, persisted through the entity UUID, including existing saves. */
    public static boolean roll(LowerLevelZbEntity z,long salt,double chance){
        return new java.util.Random(z.getUUID().getMostSignificantBits()^z.getUUID().getLeastSignificantBits()^salt).nextDouble()<chance;
    }
    public static boolean ribsVisible(LowerLevelZbEntity z){
        return applies(z)&&config().exposedRibs&&(z.isDeadOrDying()&&z.deathTime>=8||roll(z,0x52494253L,config().livingRibsChance));
    }
    public static void tick(LowerLevelZbEntity z){
        if(!applies(z) || !z.isAlive() || !(z.level() instanceof ServerLevel level) || z.tickCount%40!=0)return;
        LivingEntity target=z.getTarget();if(!validPrey(z,target) || !z.hasLineOfSight(target))return;
        int allies=0;
        for(var other:level.getEntitiesOfClass(LowerLevelZbEntity.class,z.getBoundingBox().inflate(config().packRadius))){
            if(other==z || !applies(other) || !other.isAlive() || other.getTarget()!=null || !validPrey(other,target))continue;
            if(!other.hasLineOfSight(z))continue;
            other.setTarget(target);if(++allies>=config().packAllies)break;
        }
    }
    public static void blood(ServerLevel level,Vec3 at,int count){
        if(config().bloodEffects)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.REDSTONE_BLOCK.defaultBlockState()),
                at.x,at.y,at.z,Math.min(24,count),.2,.15,.2,.08);
    }
    /** Called instead of vanilla's short death removal; vanilla die() still owns loot/XP. */
    public static void deathTick(LowerLevelZbEntity z){
        z.deathTime++;z.setGrappleTarget(-1);
        if(!(z.level() instanceof ServerLevel level))return;
        if(z.deathTime==18 && !z.horrorWormsReleased){
            z.horrorWormsReleased=true;blood(level,z.position().add(0,.65,0),24);
            z.playSound(net.minecraft.sounds.SoundEvents.SLIME_SQUISH,1.0f,.65f);
            if(config().maggotsOnDeath&&roll(z,0x4D4147474F54L,config().deathMaggotChance)){
                int worldCount=0;for(Entity e:level.getAllEntities())if(e instanceof CorpseMaggotEntity && ++worldCount>=config().worldMaggotCap)break;
                int nearby=level.getEntitiesOfClass(CorpseMaggotEntity.class,z.getBoundingBox().inflate(24)).size();
                int count=Math.max(0,Math.min(config().maggotsPerCorpse,Math.min(config().localMaggotCap-nearby,config().worldMaggotCap-worldCount)));
                for(int i=0;i<count;i++){
                    var larva=ModEntities.CORPSE_MAGGOT.create(level,EntitySpawnReason.TRIGGERED);if(larva==null)continue;
                    double angle=i*Math.PI*2/Math.max(1,count)+z.getRandom().nextDouble()*.5;
                    larva.setPos(z.getX(),z.getY()+.6,z.getZ());larva.setDeltaMovement(Math.cos(angle)*.35,.4,Math.sin(angle)*.35);
                    level.addFreshEntity(larva);
                }
            }
        }
        if(z.deathTime>=48)z.remove(Entity.RemovalReason.KILLED);
    }
}
