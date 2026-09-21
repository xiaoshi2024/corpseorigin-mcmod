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
import java.util.UUID;
/** Timed airship apparition anchoring the ninja formation; not a drivable vehicle. */
public class GreatTenguEntity extends Entity implements GeoEntity {
    private UUID owner;
    private int remaining=200;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public GreatTenguEntity(EntityType<? extends GreatTenguEntity> type,Level level){super(type,level);setNoGravity(true);}
    public void setOwner(ServerPlayer player){owner=player.getUUID();}
    @Override public void tick(){
        super.tick(); if(!(level() instanceof ServerLevel level))return;
        if(--remaining<=0 || owner==null || !(level.getEntity(owner) instanceof ServerPlayer player)
                || !player.isAlive() || !"fengmohuitailang".equals(xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(player))){discard();return;}
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
                target.hurtServer(level,damageSources().playerAttack(player),2);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,25,1));
            }
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override protected void addAdditionalSaveData(ValueOutput out){out.putString("Owner",owner==null?"":owner.toString());out.putInt("Remaining",remaining);}
    @Override protected void readAdditionalSaveData(ValueInput in){
        try{owner=UUID.fromString(in.getStringOr("Owner",""));}catch(IllegalArgumentException e){owner=null;}
        remaining=Math.min(200,in.getIntOr("Remaining",0));
    }
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
