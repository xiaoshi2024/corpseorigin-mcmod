package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.chapter.*;
import java.util.UUID;

/** Short-lived skill geometry with server-side ownership and swept projectile hits. */
public class SkillConstructEntity extends Entity implements GeoEntity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ANCHOR =
            SynchedEntityData.defineId(SkillConstructEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner,anchor;
    private String role="";
    private int life;
    public SkillConstructEntity(EntityType<? extends SkillConstructEntity> type,Level level) {super(type,level);setNoGravity(true);}
    public String kind(){return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();}
    public boolean ownedBy(ServerPlayer p){return p.getUUID().equals(owner);}
    public static SkillConstructEntity spawn(ServerPlayer p,EntityType<SkillConstructEntity> type,Entity anchor,int ticks) {
        var e=new SkillConstructEntity(type,p.level());e.owner=p.getUUID();e.anchor=anchor==null?null:anchor.getUUID();
        if(anchor!=null)e.entityData.set(ANCHOR,anchor.getId());
        e.role=CharacterManager.getInstance().getPlayerCharacterId(p);e.life=ticks;
        e.setPos(anchor==null?p.getEyePosition():anchor.position());e.setYRot(p.getYRot());
        p.level().addFreshEntity(e);return e;
    }
    @Override public void tick(){
        super.tick();
        if(level().isClientSide()){
            var attached=level().getEntity(entityData.get(ANCHOR));
            if(attached!=null)follow(attached);
            return;
        }
        if(!(level() instanceof ServerLevel level))return;
        if(--life<=0 || owner==null || !(level.getEntity(owner) instanceof ServerPlayer p)
                || !p.isAlive() || !role.equals(CharacterManager.getInstance().getPlayerCharacterId(p))){discard();return;}
        String kind=kind();
        if(kind.equals("black_gold_heart") && !xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p).hasLearned(p.getUUID(),"black_gold_heart")){discard();return;}
        if(kind.equals("tiangang_halo") && !xiaoshi2022.corpseorigin.skill.longyou.TianGangCombat.isShen(p)){discard();return;}
        if(anchor!=null){
            var target=level.getEntity(anchor);if(target==null || !target.isAlive()){discard();return;}
            if(kind.equals("vine_bind") && (!(target instanceof LivingEntity living) || !ChapterCombat.canHit(p,living))){discard();return;}
            follow(target);
            if(kind.equals("slaughter_incarnation") && tickCount%20==0)SkillRework.area(p,8,32,1);
            return;
        }
        Vec3 start=position(),end=start.add(getDeltaMovement());
        if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))){discard();return;}
        var block=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        var hit=ProjectileUtil.getEntityHitResult(this,start,block.getLocation(),new AABB(start,end).inflate(.4),
                e->e instanceof LivingEntity l && ChapterCombat.canHit(p,l),start.distanceToSqr(block.getLocation()));
        if(hit!=null){
            var target=(LivingEntity)hit.getEntity();
            float damage=kind.equals("blood_lotus_petal")?40:kind.equals("bee_wheel")?28:26;
            if(target.hurtServer(level,p.damageSources().playerAttack(p),damage)){
                var v=getDeltaMovement().normalize();target.push(v.x,.3,v.z);target.hurtMarked=true;
            }
            discard();return;
        }
        if(block.getType()!=HitResult.Type.MISS){discard();return;}
        setPos(end);setYRot(getYRot()+25);
        if(kind.equals("blood_lotus_petal")){
            ChapterCombat.dust(level,end,0xc51a36,1.8f);
            if(tickCount%8==0)level.playSound(null,getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                    net.minecraft.sounds.SoundSource.PLAYERS,.35f,1.6f);
        }
    }
    private void follow(Entity target){
        String kind=kind();
        double height=kind.equals("slaughter_incarnation")?target.getBbHeight()+.3:kind.equals("black_gold_heart")?1:0;
        setPos(target.position().add(0,height,0));
        setYRot(target instanceof LivingEntity living?living.yBodyRot:target.getYRot());
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(ANCHOR,-1);}
    @Override protected void addAdditionalSaveData(ValueOutput out){} // Ephemeral constructs never resume an attack after reload.
    @Override protected void readAdditionalSaveData(ValueInput in){life=0;}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        if(kind().equals("tiangang_halo"))controllers.add(new com.geckolib.animation.AnimationController<SkillConstructEntity>(
                "eyes",0,test->test.setAndContinue(com.geckolib.animation.RawAnimation.begin().thenLoop("openeye"))));
        if(kind().equals("black_gold_heart"))controllers.add(new com.geckolib.animation.AnimationController<SkillConstructEntity>(
                "pulse",0,test->test.setAndContinue(com.geckolib.animation.RawAnimation.begin().thenLoop("heartbeat"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
