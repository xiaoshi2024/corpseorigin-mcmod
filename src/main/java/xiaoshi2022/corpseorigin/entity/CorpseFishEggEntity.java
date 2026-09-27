package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;
import java.util.UUID;

/** A separately removable parasite. Drain stops after three seconds even on players. */
public class CorpseFishEggEntity extends Entity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final EntityDataAccessor<String> HOST = SynchedEntityData.defineId(CorpseFishEggEntity.class, EntityDataSerializers.STRING);
    private UUID owner;
    private int age, attachedTicks, slot;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public CorpseFishEggEntity(EntityType<? extends CorpseFishEggEntity> type, Level level) {
        super(type,level); setNoGravity(true);
    }
    public static CorpseFishEggEntity create(LivingEntity player, int slot) {
        var egg=new CorpseFishEggEntity(ModEntities.CORPSE_FISH_EGG,player.level());
        egg.owner=player.getUUID(); egg.slot=slot;
        egg.setPos(player.getEyePosition().add(0,-.5,0));
        egg.setDeltaMovement(player.getLookAngle().yRot((slot-1)*.22f).scale(.25));
        return egg;
    }
    public boolean attachedTo(Entity target) { return entityData.get(HOST).equals(target.getUUID().toString()); }
    public boolean ownedBy(Player player) { return player.getUUID().equals(owner); }
    private LivingEntity host(ServerLevel level) {
        try { return level.getEntity(UUID.fromString(entityData.get(HOST))) instanceof LivingEntity living ? living : null; }
        catch(IllegalArgumentException ignored) { return null; }
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        age++;
        if (owner==null || !(level.getEntity(owner) instanceof LivingEntity caster)
                || !caster.isAlive() || caster.isRemoved()) { discard(); return; }
        if (!entityData.get(HOST).isEmpty()) {
            LivingEntity target=host(level);
            if(target==null || !ChapterCombat.canHit(caster,target)) { discard(); return; }
            double angle=Math.toRadians(target.getYRot())+slot*Math.PI*2/3;
            double radius=target.getBbWidth()/2+.16;
            setPos(target.getX()+Math.cos(angle)*radius,target.getY()+target.getBbHeight()*(.35+slot*.15),
                    target.getZ()+Math.sin(angle)*radius);
            setDeltaMovement(Vec3.ZERO);
            if(attachedTicks<60 && ++attachedTicks%20==0) {
                float before=target.getHealth();
                if(target.hurtServer(level,ChapterCombat.attackSource(caster),1))
                    caster.heal(Math.min(.5f,Math.max(0,before-target.getHealth())));
                QiEffects.burst(level,getX(),getY(),getZ(),0xc0182a,2,.1);
            }
            if(attachedTicks>=60 && !(target instanceof Player)) discard();
            return;
        }
        if(age>200) { discard(); return; }
        Vec3 end=position().add(getDeltaMovement());
        if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))) { discard(); return; }
        var wall=level.clip(new ClipContext(position(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        end=wall.getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,this,position(),end,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(.15),
                e -> e instanceof LivingEntity living && ChapterCombat.canHit(caster,living),.15f);
        if(hit!=null) { attach((LivingEntity)hit.getEntity()); return; }
        setPos(end);
        setDeltaMovement(wall.getType()==HitResult.Type.MISS ? getDeltaMovement().scale(.97) : Vec3.ZERO);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.08),
                t -> ChapterCombat.canHit(caster,t))) { attach(target); break; }
    }
    private void attach(LivingEntity target) {
        var attached=level().getEntitiesOfClass(CorpseFishEggEntity.class,target.getBoundingBox().inflate(1),
                e -> e.attachedTo(target));
        if(attached.size()>=3) { discard(); return; }
        boolean[] occupied = new boolean[3];
        for (CorpseFishEggEntity egg : attached) occupied[egg.slot] = true;
        for (int index=0; index<3; index++) if (!occupied[index]) { slot=index; break; }
        entityData.set(HOST,target.getUUID().toString()); attachedTicks=0; setDeltaMovement(Vec3.ZERO);
    }
    @Override public boolean isPickable() { return true; }
    @Override public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if(player.isSpectator() || !player.isShiftKeyDown() || entityData.get(HOST).isEmpty()) return InteractionResult.PASS;
        if(!level().isClientSide()) discard();
        return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source,float amount) { return false; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(HOST,""); }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        out.putString("Owner",owner==null?"":owner.toString()); out.putString("Host",entityData.get(HOST));
        out.putInt("Age",age); out.putInt("AttachedTicks",attachedTicks); out.putInt("Slot",slot);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        try { owner=UUID.fromString(in.getStringOr("Owner","")); } catch(IllegalArgumentException ignored) { owner=null; }
        entityData.set(HOST,in.getStringOr("Host","")); age=Math.max(0,in.getIntOr("Age",0));
        attachedTicks=Math.max(0,in.getIntOr("AttachedTicks",0)); slot=Math.floorMod(in.getIntOr("Slot",0),3);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CorpseFishEggEntity>("idle",0,test->test.setAndContinue(IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
