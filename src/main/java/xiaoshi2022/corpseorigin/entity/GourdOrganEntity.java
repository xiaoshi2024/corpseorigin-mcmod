package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

/** Detached living organ; attachment is rendered directly on the player. */
public final class GourdOrganEntity extends PathfinderMob implements GeoEntity, OwnerBound, ZombieKin {
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(GourdOrganEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FORM=SynchedEntityData.defineId(GourdOrganEntity.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int animationTicks;
    private boolean staying;
    private int storedFlesh;
    public boolean staying(){return staying;}
    public int storedFlesh(){return storedFlesh;}
    public void storeFlesh(int amount){storedFlesh=(int)Math.clamp((long)storedFlesh+Math.max(0,amount),0,xiaoshi2022.corpseorigin.skill.chapter.GourdBalance.PET_CAPACITY);}
    public void restoreStoredFlesh(int amount){storedFlesh=Math.clamp(amount,0,xiaoshi2022.corpseorigin.skill.chapter.GourdBalance.PET_CAPACITY);}
    public boolean petPrey(ServerPlayer p,LivingEntity t){
        return (t instanceof net.minecraft.world.entity.npc.villager.AbstractVillager||t instanceof net.minecraft.world.entity.monster.zombie.Zombie)
            && !(t instanceof OwnerBound) && !t.hasCustomName() && !t.isBaby() && !t.isPassenger() && !t.isVehicle()
            && ChapterCombat.canHit(p,t) && xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.edible(t)
            && distanceToSqr(t)<=36 && hasLineOfSight(t);
    }
    @Override public net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(!isOwnedBy(player))return super.mobInteract(player,hand);
        if(player instanceof ServerPlayer p && hand==net.minecraft.world.InteractionHand.MAIN_HAND && GourdOrganState.isCurrent(p,this)){
            if(player.isShiftKeyDown()){
                if(GourdOrganState.windingUp(p))p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("skill.corpseorigin.gourd_devour.busy"));
                else GourdOrganState.toggle(p);
            }else{
                staying=!staying;setTarget(null);getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if(staying)xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.cancel(p);
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(staying?"message.corpseorigin.gourd.stay":"message.corpseorigin.gourd.follow",storedFlesh,xiaoshi2022.corpseorigin.skill.chapter.GourdBalance.PET_CAPACITY));
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    private boolean mayAct(){if(!(level() instanceof ServerLevel l))return false;var p=owner(l);return !staying&&p!=null&&p.isAlive()&&!p.isSpectator()&&p.level()==l&&GourdOrganState.active(p)&&GourdOrganState.detached(p)&&GourdOrganState.isCurrent(p,this)&&!GourdOrganState.windingUp(p);}
    public GourdOrganEntity(EntityType<? extends GourdOrganEntity> type,Level level){super(type,level);setPersistenceRequired();xpReward=0;}
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(OWNER,"");b.define(FORM,0);}
    public void setOwner(ServerPlayer p){entityData.set(OWNER,p.getUUID().toString());}
    @Override public boolean isOwnedBy(Entity e){return e!=null && entityData.get(OWNER).equals(e.getUUID().toString());}
    public int form(){return entityData.get(FORM);}
    public void play(int form,int ticks){entityData.set(FORM,form);animationTicks=ticks;}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,true){@Override public boolean canUse(){return mayAct()&&super.canUse();}@Override public boolean canContinueToUse(){return mayAct()&&super.canContinueToUse();}});goalSelector.addGoal(5,new RandomLookAroundGoal(this));}
    private ServerPlayer owner(ServerLevel level){try{return level.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(entityData.get(OWNER)));}catch(IllegalArgumentException e){return null;}}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level))return;
        if(entityData.get(OWNER).isEmpty()){discard();return;}
        if(animationTicks>0 && --animationTicks==0)entityData.set(FORM,0);
        var p=owner(level);if(p==null){setTarget(null);getNavigation().stop();return;}
        if(!GourdOrganState.detached(p) || !GourdOrganState.active(p) || !GourdOrganState.isCurrent(p,this)){discard();return;}
        if(p.level()!=level || !p.isAlive() || p.isSpectator()){setTarget(null);getNavigation().stop();return;}
        GourdOrganState.saveHealth(p,getHealth());
        if(staying||GourdOrganState.windingUp(p)){setTarget(null);getNavigation().stop();return;}
        if(tickCount%20==0 && distanceToSqr(p)<=576 && storedFlesh<xiaoshi2022.corpseorigin.skill.chapter.GourdBalance.PET_CAPACITY
                && level.getGameTime()>=p.getAttachedOrCreate(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY).getLongOr("gourd_pet_next_feed",0)){
            var prey=level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(6),t->petPrey(p,t)).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if(prey!=null && xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.beginPet(p,this,prey)){
                var body=p.getAttachedOrCreate(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY).copy();body.putLong("gourd_pet_next_feed",level.getGameTime()+xiaoshi2022.corpseorigin.skill.chapter.GourdBalance.PET_COOLDOWN);p.setAttached(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY,body);
                setTarget(null);getNavigation().stop();return;
            }
        }
        var target=p.getLastHurtMob();if(target==null || !target.isAlive() || distanceToSqr(target)>576)target=p.getLastHurtByMob();
        if(target!=null && target.level()==level && distanceToSqr(target)<576 && ChapterCombat.canHit(p,target))setTarget(target);
        else if(getTarget()!=null && (!ChapterCombat.canHit(p,getTarget()) || distanceToSqr(getTarget())>576))setTarget(null);
        if(getTarget()==null && tickCount%10==0){if(distanceToSqr(p)>144){var at=p.position();if(level.noCollision(this,getBoundingBox().move(at.subtract(position()))))setPos(at);}else if(distanceToSqr(p)>9)getNavigation().moveTo(p,1);else getNavigation().stop();}
    }
    @Override public boolean doHurtTarget(ServerLevel level,Entity target){var p=owner(level);if(!mayAct() || p==null || !(target instanceof LivingEntity living) || !ChapterCombat.canHit(p,living))return false;play(2,49);return super.doHurtTarget(level,target);}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){if(owner(level)==null || isOwnedBy(source.getEntity()))return false;return super.hurtServer(level,source,amount);}
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){if(level() instanceof ServerLevel level){var p=owner(level);if(p!=null && GourdOrganState.isCurrent(p,this))GourdOrganState.killed(p);}super.die(source);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putString("GourdOwner",entityData.get(OWNER));out.putBoolean("GourdStay",staying);out.putInt("GourdFlesh",storedFlesh);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(OWNER,in.getStringOr("GourdOwner",""));staying=in.getBooleanOr("GourdStay",false);restoreStoredFlesh(in.getIntOr("GourdFlesh",0));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<GourdOrganEntity>("form",0,s->s.setAndContinue(RawAnimation.begin().thenLoop(GourdOrganState.clip(form(),true)))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
