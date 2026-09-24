package xiaoshi2022.corpseorigin.entity;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

/** Detached living organ; attachment is rendered directly on the player. */
public final class GourdOrganEntity extends PathfinderMob implements GeoEntity, OwnerBound, ZombieKin {
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(GourdOrganEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FORM=SynchedEntityData.defineId(GourdOrganEntity.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int animationTicks;
    public GourdOrganEntity(EntityType<? extends GourdOrganEntity> type,Level level){super(type,level);setPersistenceRequired();xpReward=0;}
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(OWNER,"");b.define(FORM,0);}
    public void setOwner(ServerPlayer p){entityData.set(OWNER,p.getUUID().toString());}
    @Override public boolean isOwnedBy(Entity e){return e!=null && entityData.get(OWNER).equals(e.getUUID().toString());}
    public int form(){return entityData.get(FORM);}
    public void play(int form,int ticks){entityData.set(FORM,form);animationTicks=ticks;}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,true));goalSelector.addGoal(5,new RandomLookAroundGoal(this));}
    private ServerPlayer owner(ServerLevel level){try{return level.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(entityData.get(OWNER)));}catch(IllegalArgumentException e){return null;}}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level))return;
        if(entityData.get(OWNER).isEmpty()){discard();return;}
        if(animationTicks>0 && --animationTicks==0)entityData.set(FORM,0);
        var p=owner(level);if(p==null){setTarget(null);getNavigation().stop();return;}
        if(!GourdOrganState.detached(p) || !GourdOrganState.active(p) || !GourdOrganState.isCurrent(p,this)){discard();return;}
        if(p.level()!=level || !p.isAlive() || p.isSpectator()){setTarget(null);getNavigation().stop();return;}
        GourdOrganState.saveHealth(p,getHealth());
        var target=p.getLastHurtMob();if(target==null || !target.isAlive() || distanceToSqr(target)>576)target=p.getLastHurtByMob();
        if(target!=null && target.level()==level && distanceToSqr(target)<576 && ChapterCombat.canHit(p,target))setTarget(target);
        else if(getTarget()!=null && (!ChapterCombat.canHit(p,getTarget()) || distanceToSqr(getTarget())>576))setTarget(null);
        if(getTarget()==null && tickCount%10==0){if(distanceToSqr(p)>144){var at=p.position();if(level.noCollision(this,getBoundingBox().move(at.subtract(position()))))setPos(at);}else if(distanceToSqr(p)>9)getNavigation().moveTo(p,1);else getNavigation().stop();}
    }
    @Override public boolean doHurtTarget(ServerLevel level,Entity target){var p=owner(level);if(p==null || GourdOrganState.windingUp(p) || !(target instanceof LivingEntity living) || !ChapterCombat.canHit(p,living))return false;play(2,49);return super.doHurtTarget(level,target);}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){if(owner(level)==null || isOwnedBy(source.getEntity()))return false;return super.hurtServer(level,source,amount);}
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){if(level() instanceof ServerLevel level){var p=owner(level);if(p!=null && GourdOrganState.isCurrent(p,this))GourdOrganState.killed(p);}super.die(source);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putString("GourdOwner",entityData.get(OWNER));}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(OWNER,in.getStringOr("GourdOwner",""));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<GourdOrganEntity>("form",0,s->s.setAndContinue(RawAnimation.begin().thenLoop(GourdOrganState.clip(form(),true)))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
