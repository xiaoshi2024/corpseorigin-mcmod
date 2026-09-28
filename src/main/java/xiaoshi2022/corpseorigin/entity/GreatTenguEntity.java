package xiaoshi2022.corpseorigin.entity;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.*;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;
import java.util.UUID;
import xiaoshi2022.corpseorigin.skill.longyou.UndeadBodyState;
/** Timed airship apparition anchoring the ninja formation; not a drivable vehicle. */
public class GreatTenguEntity extends Entity implements GeoEntity {
    private UUID owner;
    private int remaining=200;
    private int phase;
    private boolean laserCalled;
    private UUID laserTarget;
    private int laserCooldown;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public GreatTenguEntity(EntityType<? extends GreatTenguEntity> type,Level level){super(type,level);setNoGravity(true);}
    public void setOwner(net.minecraft.world.entity.LivingEntity player){owner=player.getUUID();}
    public boolean isOwnedBy(net.minecraft.world.entity.LivingEntity player){return owner!=null && owner.equals(player.getUUID());}
    @Override public void tick(){
        super.tick(); if(!(level() instanceof ServerLevel level))return;
        if(laserCooldown>0)laserCooldown--;
        if(--remaining<=0 || owner==null || !(level.getEntity(owner) instanceof net.minecraft.world.entity.LivingEntity player)
                || !player.isAlive() || !"fengmohuitailang".equals(ChapterCombat.actorRole(player))){discard();return;}
        if(phase==2){
            if(remaining==100 && player instanceof ServerPlayer serverPlayer) UndeadBodyState.evolveInside(serverPlayer);
            if(remaining%10==0) ChapterCombat.ring(level,position(),5,0x31e6e8,32);
            if(remaining%20==0 && player.getAttachedOrCreate(UndeadBodyState.STATE)==3) player.hurtServer(level,damageSources().generic(),1);
            return;
        }
        if(laserCalled){
            laserCalled=false;
            var entity=laserTarget==null?ChapterCombat.aim(player,64):level.getEntity(laserTarget);
            laserTarget=null;
            if(entity instanceof net.minecraft.world.entity.LivingEntity target && ChapterCombat.canHit(player,target)
                    && player.distanceToSqr(target)<=4096 && player.hasLineOfSight(target)){
                for(int i=0;i<48;i++) ChapterCombat.dust(level,position().lerp(target.getEyePosition(),i/48.0),0x21e6e6,2);
                target.hurtServer(level,xiaoshi2022.corpseorigin.skill.QiSkillDamageSource.wrap(ChapterCombat.attackSource(player)),48);
            }
        }
        if(remaining%10!=0)return;
        var center=position().add(0,-5,0);
        ChapterCombat.ring(level,center,6,0xcc223b,48);
        ChapterCombat.ring(level,center,4.8,0xe2bc66,32);
        for(int i=0;i<6;i++) {
            double angle=i*Math.PI/3;
            for(int j=0;j<12;j++)ChapterCombat.dust(level,center.add(Math.cos(angle)*j*.5,.1,Math.sin(angle)*j*.5),0xcc223b,1);
        }
        if(remaining%20!=0)return;
        for(var target:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(6)))
            if(ChapterCombat.canHit(player,target) && target.distanceToSqr(center)<=36 && player.hasLineOfSight(target)){
                target.hurtServer(level,xiaoshi2022.corpseorigin.skill.QiSkillDamageSource.wrap(ChapterCombat.attackSource(player)),2);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,25,1));
            }
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override protected void addAdditionalSaveData(ValueOutput out){out.putString("Owner",owner==null?"":owner.toString());out.putInt("Remaining",remaining);out.putInt("Phase",phase);}
    /** Explicitly called by the ninja leader; no timed firing. */
    public void callLaser() { if ((phase == 0 || phase == 1) && laserCooldown==0) {laserCalled = true;laserCooldown=40;} }
    public boolean callLaser(net.minecraft.world.entity.LivingEntity target) {
        if(laserCooldown>0 || phase>1)return false;
        laserTarget=target.getUUID();callLaser();return true;
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        try{owner=UUID.fromString(in.getStringOr("Owner",""));}catch(IllegalArgumentException e){owner=null;}
        remaining=Math.min(200,in.getIntOr("Remaining",0)); phase=in.getIntOr("Phase",0);
    }
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
        if(phase==2 && source.getEntity() instanceof ServerPlayer attacker && attacker.getAttachedOrCreate(UndeadBodyState.STATE)==4){
            if(amount>=4){phase=3; remaining=10; UndeadBodyState.escapeShip(attacker); attacker.teleportTo(getX(),getY()+2,getZ()); QiEffects.burst(level,getX(),getY(),getZ(),0xd8552c,30,2);}
            return true;
        }
        return false;
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}

