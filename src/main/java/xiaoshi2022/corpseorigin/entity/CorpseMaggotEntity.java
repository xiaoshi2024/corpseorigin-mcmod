package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.growth.CorpseHorror;

/** A separate hostile larva; does not replace the existing lore-specific ZbWorm. */
public final class CorpseMaggotEntity extends PathfinderMob implements GeoEntity,ZombieKin {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("idle"),CRAWL=RawAnimation.begin().thenLoop("crawl"),BITE=RawAnimation.begin().thenLoop("bite"),EMERGE=RawAnimation.begin().thenPlay("emerge");
    private int lifetime;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> HOST=net.minecraft.network.syncher.SynchedEntityData.defineId(CorpseMaggotEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ATTACHED_AGE=net.minecraft.network.syncher.SynchedEntityData.defineId(CorpseMaggotEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> BURROW_DURATION=net.minecraft.network.syncher.SynchedEntityData.defineId(CorpseMaggotEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final RawAnimation BURROW=RawAnimation.begin().thenPlay("burrow"),FEED=RawAnimation.begin().thenLoop("attached_feed");
    private int reattachAfter;
    private boolean infectedHost;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HOST,-1);b.define(ATTACHED_AGE,0);b.define(BURROW_DURATION,30);}
    public int getHostId(){return entityData.get(HOST);}
    public int getAttachedAge(){return entityData.get(ATTACHED_AGE);}
    public boolean tryAttach(LivingEntity host){
        var cfg=CorpseHorror.config();
        if(level().isClientSide() || !cfg.enabled || !cfg.maggotParasitism || tickCount<reattachAfter || getHostId()>=0
                || !xiaoshi2022.corpseorigin.effect.BYeffect.canInfect(host) || !CorpseHorror.validPrey(this,host)
                || distanceToSqr(host)>2.25 || !hasLineOfSight(host))return false;
        if(level().getEntitiesOfClass(CorpseMaggotEntity.class,host.getBoundingBox().inflate(3),m->m.getHostId()==host.getId()).size()>=cfg.maggotsPerHost)return false;
        entityData.set(HOST,host.getId());entityData.set(ATTACHED_AGE,0);entityData.set(BURROW_DURATION,cfg.maggotBurrowTicks);infectedHost=false;
        getNavigation().stop();setNoGravity(true);setTarget(null);return true;
    }
    public void detach(){entityData.set(HOST,-1);entityData.set(ATTACHED_AGE,0);setNoGravity(false);reattachAfter=tickCount+80;setDeltaMovement(0,.18,0);}
    private void tickAttachment(){
        if(!(level() instanceof net.minecraft.server.level.ServerLevel server) || getHostId()<0)return;
        var entity=level().getEntity(getHostId());var cfg=CorpseHorror.config();
        if(!(entity instanceof LivingEntity host) || !cfg.enabled || !cfg.maggotParasitism || !CorpseHorror.validPrey(this,host)
                || !xiaoshi2022.corpseorigin.effect.BYeffect.canInfect(host) || hurtTime>0 || isOnFire() || isInWater()
                || (host instanceof Player p && p.isShiftKeyDown() && !p.onGround())
                || (infectedHost && !host.hasEffect(xiaoshi2022.corpseorigin.registry.ModEffects.QIANS))){detach();return;}
        int age=getAttachedAge()+1;entityData.set(ATTACHED_AGE,age);getNavigation().stop();setTarget(null);
        double angle=Math.toRadians(host.getYRot())+(getId()%2==0?1:-1)*.7;
        double depth=age<cfg.maggotBurrowTicks?Math.sin(Math.PI*age/cfg.maggotBurrowTicks)*.12:0;
        double radius=host.getBbWidth()*.5+.07-depth;
        setPos(host.getX()-Math.sin(angle)*radius,host.getY()+host.getBbHeight()*(.5+(getId()%3)*.08),host.getZ()+Math.cos(angle)*radius);
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);setYRot(host.getYRot()+180);setYBodyRot(getYRot());
        if(age>=cfg.maggotBurrowTicks && age%20==0){
            if(host.hurtServer(server,damageSources().mobAttack(this),1))CorpseHorror.blood(server,position(),4);
        }
        if(age>=cfg.maggotInfectionTicks && !infectedHost){
            xiaoshi2022.corpseorigin.effect.BYeffect.applyInfection(host,server,600,null);infectedHost=true;
            if(host instanceof net.minecraft.server.level.ServerPlayer p)p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.maggot_infection"));
        }
    }
    public CorpseMaggotEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);xpReward=0;}
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,4).add(Attributes.ATTACK_DAMAGE,1).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MeleeAttackGoal(this,1.25,true));goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.8));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<Player>(this,Player.class,10,true,false,(t,l)->CorpseHorror.validPrey(this,t) && !ZombieKin.isZombieKin(t)));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<net.minecraft.world.entity.npc.villager.Villager>(this,net.minecraft.world.entity.npc.villager.Villager.class,true));
    }
    @Override public void tick(){super.tick();if(!level().isClientSide()){tickAttachment();if(++lifetime>CorpseHorror.config().maggotLifetimeTicks)discard();}}
    @Override public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level,Entity target){
        if(!(target instanceof LivingEntity living) || !CorpseHorror.validPrey(this,living))return false;
        if(getHostId()>=0)return false;
        if(tryAttach(living))return true;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);return super.doHurtTarget(level,target);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<CorpseMaggotEntity>("body",2,t->{
            boolean burrowing=getHostId()>=0 && getAttachedAge()<entityData.get(BURROW_DURATION);
            t.setControllerSpeed(burrowing?30f/Math.max(1,entityData.get(BURROW_DURATION)):1f);
            return t.setAndContinue(getHostId()>=0?(burrowing?BURROW:FEED):tickCount<12?EMERGE:swinging?BITE:t.isMoving()?CRAWL:IDLE);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("HorrorLifetime",lifetime);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);lifetime=Math.max(0,in.getIntOr("HorrorLifetime",0));setNoGravity(false);}
}
