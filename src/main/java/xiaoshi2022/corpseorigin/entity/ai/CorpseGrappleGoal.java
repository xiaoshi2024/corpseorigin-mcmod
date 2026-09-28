package xiaoshi2022.corpseorigin.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.growth.CorpseHorror;

import java.util.EnumSet;

/** Short, breakable close-contact grapple; no forced riding or player input suppression. */
public final class CorpseGrappleGoal extends Goal {
    private final LowerLevelZbEntity mob;
    private LivingEntity prey;
    private int started,next;
    private Vec3 side=Vec3.ZERO;
    public CorpseGrappleGoal(LowerLevelZbEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        if(!CorpseHorror.applies(mob) || mob.tickCount<next || mob.hurtTime>0)return false;
        prey=mob.getTarget();
        if(!CorpseHorror.validPrey(mob,prey) || mob.distanceToSqr(prey)>2.4 || !mob.hasLineOfSight(prey))return false;
        return mob.level().getEntitiesOfClass(LowerLevelZbEntity.class,prey.getBoundingBox().inflate(3),
                z->z.isAlive() && z.getGrappleTarget()==prey.getId()).size()<CorpseHorror.config().maxGrapplers;
    }
    @Override public void start(){
        started=mob.tickCount;mob.getNavigation().stop();mob.setGrappleTarget(prey.getId());
        side=new Vec3(mob.getX()-prey.getX(),0,mob.getZ()-prey.getZ()).normalize();
        if(side.lengthSqr()<.01){double a=mob.getId()*2.4;side=new Vec3(Math.cos(a),0,Math.sin(a));}
    }
    @Override public boolean canContinueToUse(){
        return prey!=null && mob.getGrappleTarget()==prey.getId() && CorpseHorror.applies(mob) && CorpseHorror.validPrey(mob,prey) && mob.hurtTime==0
                && mob.tickCount-started<CorpseHorror.config().grappleTicks && mob.distanceToSqr(prey)<6.25
                && mob.hasLineOfSight(prey) && !(prey instanceof Player p && p.isShiftKeyDown() && !p.onGround());
    }
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        if(prey==null)return;
        mob.getLookControl().setLookAt(prey,30,30);
        Vec3 desired=prey.position().add(side.scale((prey.getBbWidth()+mob.getBbWidth())*.48));
        Vec3 pull=desired.subtract(mob.position());
        if(pull.lengthSqr()<6.25 && mob.level().noCollision(mob,mob.getBoundingBox().move(pull.scale(.35))))
            mob.setDeltaMovement(pull.scale(.35).add(0,mob.getDeltaMovement().y,0));
        prey.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,6,0,false,false,true));
        if((mob.tickCount-started)%20==5 && mob.getRandom().nextDouble()<CorpseHorror.config().grappleEscapeChance){
            stop();return;
        }
        if((mob.tickCount-started)%20==8 && mob.level() instanceof ServerLevel level){
            if(prey.hurtServer(level,mob.damageSources().mobAttack(mob),(float)(mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*.65))){
                mob.setHunger(mob.getHunger()+8);CorpseHorror.blood(level,prey.position().add(0,prey.getBbHeight()*.65,0),8);
                mob.playSound(xiaoshi2022.corpseorigin.registry.ModSounds.GROUND_CHI,.8f,.75f);
            }
        }
    }
    @Override public void stop(){mob.setGrappleTarget(-1);next=mob.tickCount+CorpseHorror.config().grappleCooldownTicks;prey=null;}
}
