package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;
import java.util.UUID;

/** Short-lived summons; owner, team and line-of-sight checks apply to every hit. */
public class CorpseAntEntity extends PathfinderMob implements GeoEntity, ZombieKin {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner;
    private int remaining=400;
    public CorpseAntEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    public void setOwner(net.minecraft.world.entity.LivingEntity p){owner=p.getUUID();}
    public boolean ownedBy(net.minecraft.world.entity.LivingEntity p){return p.getUUID().equals(owner);}
    public boolean isBullet(){return getType()==ModEntities.BULLET_ANT;}
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,.32).add(Attributes.FOLLOW_RANGE,18).add(Attributes.ATTACK_DAMAGE,3);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));}
    @Override protected void customServerAiStep(ServerLevel level){
        if(owner==null)return;
        if(--remaining<=0 || !(level.getEntity(owner) instanceof LivingEntity p) || !p.isAlive()
                || !"chongmu".equals(ChapterCombat.actorRole(p)) || distanceToSqr(p)>1600){discard();return;}
        LivingEntity target=level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(14),
                e->!(e instanceof CorpseAntEntity) && ChapterCombat.canHit(p,e) && hasLineOfSight(e))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(target);
        if(target==null){if(distanceToSqr(p)>9)getNavigation().moveTo(p,1);return;}
        getNavigation().moveTo(target,1.2);
        if(tickCount%20!=0 || distanceToSqr(target)>3.2 || !hasLineOfSight(target))return;
        triggerAnim("action","attack");
        if(target.hurtServer(level,ChapterCombat.attackSource(p),isBullet()?4:2)) {
            if(!isBullet())target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));
            else {
                QiEffects.burst(level,getX(),getY()+.4,getZ(),0xd8552c,1,0);
                for(LivingEntity nearby:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(2)))
                    if(nearby!=target && !(nearby instanceof CorpseAntEntity) && ChapterCombat.canHit(p,nearby) && hasLineOfSight(nearby))nearby.hurtServer(level,ChapterCombat.attackSource(p),2);
            }
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putString("Summoner",owner==null?"":owner.toString());out.putInt("Remaining",remaining);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);try{owner=UUID.fromString(in.getStringOr("Summoner",""));}catch(IllegalArgumentException e){owner=null;}remaining=Math.max(0,Math.min(400,in.getIntOr("Remaining",0)));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<CorpseAntEntity>("movement",3,t->t.setAndContinue(RawAnimation.begin().thenLoop(t.isMoving()?"walk":"idle"))));
        controllers.add(new AnimationController<CorpseAntEntity>("action",1,t->com.geckolib.animation.object.PlayState.STOP).triggerableAnim("attack",RawAnimation.begin().thenPlay("attack")));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
