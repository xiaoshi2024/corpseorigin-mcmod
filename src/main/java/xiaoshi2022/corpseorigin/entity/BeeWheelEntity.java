package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.item.BeeWheelItem;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

import java.util.UUID;

/** Server-authoritative hook. Movement uses velocity so world collisions remain effective. */
public class BeeWheelEntity extends SkillConstructEntity {
    public static final int FLYING=0, BLOCK=1, TARGET=2, RETURNING=3;
    public static final double RANGE=32;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(BeeWheelEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID=SynchedEntityData.defineId(BeeWheelEntity.class,EntityDataSerializers.INT);
    private static final RawAnimation SHOOT=RawAnimation.begin().thenPlayAndHold("shoot");
    private static final RawAnimation GRAB=RawAnimation.begin().thenPlayAndHold("grab");
    private static final RawAnimation RETRACT=RawAnimation.begin().thenPlayAndHold("retract");
    private UUID targetId;
    private BlockPos latchBlock;
    private ItemStack launcher=ItemStack.EMPTY;
    private long launcherId;
    private boolean reeling;
    private int phaseTicks;
    private double ropeLength;
    private Vec3 landingPoint;

    public BeeWheelEntity(EntityType<? extends SkillConstructEntity> type,Level level){
        super(type,level);
        setNoAi(true);
    }
    // The hook performs its own swept collision check and position update below.
    // Mob travel would move it first, skip collisions, and damp its launch velocity.
    @Override public void travel(Vec3 input){}
    @Override public boolean isPushable(){return false;}
    @Override public void push(double x,double y,double z){}
    public void initialize(ServerPlayer player){
        entityData.set(OWNER_ID,player.getId());
        launcher=player.getMainHandItem();
        launcherId=GeoItem.getOrAssignId(launcher,player.level());
        itemAnimation(player,"shoot");
    }
    public Entity ropeOwner(){return level().getEntity(entityData.get(OWNER_ID));}
    public void initializeClone(CloneAvatarEntity clone) {
        entityData.set(OWNER_ID, clone.getId());
        launcher = clone.getMainHandItem();
    }
    /** AI reels a successfully hooked enemy in, then returns the actual wheel to its holder. */
    public void tickCloneWheel(ServerLevel level, CloneAvatarEntity clone) {
        phaseTicks++;
        Vec3 hand = clone.getEyePosition().add(0, -.25, 0);
        if (!clone.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.BEE_WHEEL)
                || hand.distanceToSqr(position()) > RANGE * RANGE || phaseTicks > 80)
            entityData.set(PHASE, RETURNING);
        if (phase() == RETURNING) {
            Vec3 delta = hand.subtract(position());
            if (delta.lengthSqr() < 2) { discard(); return; }
            setPos(position().add(delta.normalize().scale(Math.min(2, delta.length()))));
            return;
        }
        if (phase() == TARGET) {
            if (!(level.getEntity(targetId) instanceof LivingEntity target) || !ChapterCombat.canHit(clone, target)
                    || !clone.hasLineOfSight(target)) { entityData.set(PHASE, RETURNING); return; }
            setPos(target.getBoundingBox().getCenter());
            Vec3 pull = hand.subtract(position());
            if (pull.lengthSqr() < 4) { entityData.set(PHASE, RETURNING); return; }
            target.setDeltaMovement(target.getDeltaMovement().scale(.5).add(pull.normalize().scale(.45)));
            target.hurtMarked = true;
            return;
        }
        Vec3 start = position(), end = start.add(getDeltaMovement());
        if (!level.hasChunkAt(BlockPos.containing(end))) { entityData.set(PHASE, RETURNING); return; }
        var wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var hit = ProjectileUtil.getEntityHitResult(this, start, wall.getLocation(), new AABB(start, end).inflate(.35),
                e -> e instanceof LivingEntity living && !(e instanceof SkillConstructEntity)
                        && ChapterCombat.canHit(clone, living), start.distanceToSqr(wall.getLocation()));
        if (hit != null) {
            var target = (LivingEntity) hit.getEntity();
            target.hurtServer(level, clone.damageSources().mobAttack(clone), 28);
            targetId = target.getUUID();
            setPos(hit.getLocation());
            setDeltaMovement(Vec3.ZERO);
            entityData.set(PHASE, TARGET);
        } else if (wall.getType() != HitResult.Type.MISS) entityData.set(PHASE, RETURNING);
        else setPos(end);
    }
    public void tickDoctorWheel(ServerLevel level, TianDoctorEntity doctor) {
        Vec3 start = position(), end = start.add(getDeltaMovement());
        if (!level.hasChunkAt(BlockPos.containing(end))) { discard(); return; }
        var wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var hit = ProjectileUtil.getEntityHitResult(this, start, wall.getLocation(), new AABB(start, end).inflate(.35),
                entity -> entity instanceof LivingEntity living && ZombieKin.isZombieKin(living)
                        && !ZombieKin.isZombieKing(living), start.distanceToSqr(wall.getLocation()));
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            float damage = target instanceof MultiHeadCorpseWormEntity
                    ? Math.max(28.0F, target.getHealth()) : 28.0F;
            target.hurtServer(level, doctor.damageSources().mobAttack(doctor), damage);
            target.setDeltaMovement(target.getDeltaMovement().add(getDeltaMovement().normalize().scale(.25)));
            target.hurtMarked = true;
            discard();
        } else if (wall.getType() != HitResult.Type.MISS) discard();
        else {
            setPos(end);
            setYRot(getYRot() + 25);
        }
    }
    public int phase(){return entityData.get(PHASE);}
    public static BeeWheelEntity active(ServerPlayer player){
        var entity=findOwned(player,"bee_wheel");
        return entity instanceof BeeWheelEntity wheel?wheel:null;
    }
    public void control(ServerPlayer player){
        if(!ownedBy(player))return;
        if(player.isShiftKeyDown()){retract(player);return;}
        // Repeated use packets from holding right-click must never cancel a launch or reel.
        if((phase()==BLOCK || phase()==TARGET) && !reeling){
            beginReeling(player);
        }
    }
    private void beginReeling(ServerPlayer player){
        if(reeling)return;
        reeling=true;
        if(phase()==BLOCK)landingPoint=findLandingPoint(player);
        itemAnimation(player,"retract");
    }
    private Vec3 findLandingPoint(ServerPlayer player){
        if(latchBlock==null)return null;
        // Find the top of the hooked trunk/canopy, but never climb an unlimited wall.
        for(int height=0;height<=12;height++){
            BlockPos support=latchBlock.above(height);
            if(!player.level().hasChunkAt(support))return null;
            var shape=player.level().getBlockState(support).getCollisionShape(player.level(),support);
            if(shape.isEmpty())continue;
            Vec3 feet=new Vec3(support.getX()+.5,support.getY()+shape.max(net.minecraft.core.Direction.Axis.Y)+.08,support.getZ()+.5);
            if(player.level().noCollision(player,player.getBoundingBox().move(feet.subtract(player.position()))))return feet;
        }
        return null;
    }
    private void itemAnimation(ServerPlayer player,String name){
        if(launcher.getItem() instanceof BeeWheelItem item){
            item.stopTriggeredAnim(player,launcherId,"main",null);
            item.triggerAnim(player,launcherId,"main",name);
        }
    }
    private void changePhase(ServerPlayer player,int phase){
        entityData.set(PHASE,phase);phaseTicks=0;
        itemAnimation(player,phase==RETURNING?"retract":"grab");
    }
    private void retract(ServerPlayer player){
        if(phase()==RETURNING)return;
        targetId=null;latchBlock=null;landingPoint=null;reeling=false;
        setDeltaMovement(Vec3.ZERO);changePhase(player,RETURNING);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(PHASE,FLYING);builder.define(OWNER_ID,-1);
    }
    @Override protected boolean tickConstruct(ServerLevel level,ServerPlayer player){
        phaseTicks++;
        var held=player.getMainHandItem();
        boolean holdingLauncher=held.getItem() instanceof BeeWheelItem && GeoItem.getId(held)==launcherId;
        if(player.isSpectator() || player.isPassenger() || !holdingLauncher || player.isShiftKeyDown())retract(player);
        Vec3 hand=player.getEyePosition().add(0,-.25,0);
        if(position().distanceToSqr(hand)>RANGE*RANGE+64)retract(player);
        if(phase()==RETURNING){
            Vec3 delta=hand.subtract(position());
            if(delta.length()<1.4 || phaseTicks>40){discard();return true;}
            setPos(position().add(delta.normalize().scale(Math.min(2.4,delta.length()))));
            return true;
        }
        if(phase()==FLYING){
            Vec3 start=position(),end=start.add(getDeltaMovement());
            if(!level.hasChunkAt(BlockPos.containing(end)) || hand.distanceToSqr(end)>RANGE*RANGE){retract(player);return true;}
            var wall=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            var hit=ProjectileUtil.getEntityHitResult(this,start,wall.getLocation(),new AABB(start,end).inflate(.35),
                    e->e instanceof LivingEntity living && !(e instanceof SkillConstructEntity)
                            && ChapterCombat.canHit(player,living),start.distanceToSqr(wall.getLocation()));
            if(hit!=null){
                var target=(LivingEntity)hit.getEntity();
                setPos(hit.getLocation());setDeltaMovement(Vec3.ZERO);
                if(!target.hurtServer(level,player.damageSources().playerAttack(player),28) || !target.isAlive()){retract(player);return true;}
                targetId=target.getUUID();ropeLength=hand.distanceTo(position());changePhase(player,TARGET);
            }else if(wall.getType()!=HitResult.Type.MISS){
                latchBlock=wall.getBlockPos();
                setPos(wall.getLocation().add(Vec3.atLowerCornerOf(wall.getDirection().getUnitVec3i()).scale(.08)));
                setDeltaMovement(Vec3.ZERO);ropeLength=hand.distanceTo(position());changePhase(player,BLOCK);
            }else setPos(end);
            return true;
        }
        LivingEntity target=null;
        if(phase()==TARGET){
            if(!(level.getEntity(targetId) instanceof LivingEntity living) || !ChapterCombat.canHit(player,living)){
                retract(player);return true;
            }
            target=living;setPos(target.getBoundingBox().getCenter());
        }else if(latchBlock==null || !level.hasChunkAt(latchBlock)
                || level.getBlockState(latchBlock).getCollisionShape(level,latchBlock).isEmpty()){
            retract(player);return true;
        }
        // Space is the natural grapple-launch control, including while airborne.
        if(phase()==BLOCK && player.getLastClientInput().jump())beginReeling(player);
        var obstruction=level.clip(new ClipContext(hand,position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(obstruction.getType()!=HitResult.Type.MISS && (latchBlock==null || !obstruction.getBlockPos().equals(latchBlock))){
            retract(player);return true;
        }
        double distance=hand.distanceTo(position());
        if(phaseTicks>200 || (reeling && target!=null && distance<2)){retract(player);return true;}
        if(reeling && target==null && landingPoint!=null){
            if(!level.noCollision(player,player.getBoundingBox().move(landingPoint.subtract(player.position())))){
                landingPoint=null;
            }else{
                Vec3 feetDelta=landingPoint.subtract(player.position());
                if(feetDelta.horizontalDistance()<.6 && player.getY()>=landingPoint.y-.15){retract(player);return true;}
                // Lift the feet over the lip first, then steer onto the top. Vanilla collision remains active.
                boolean belowLip=player.getY()<landingPoint.y+.2;
                Vec3 direction=belowLip?new Vec3(feetDelta.x,Math.max(2,feetDelta.y),feetDelta.z):feetDelta;
                Vec3 velocity=BeeWheelPhysics.climbVelocity(player.getDeltaMovement(),direction,belowLip,player.horizontalCollision);
                player.setDeltaMovement(velocity);player.hurtMarked=true;player.resetFallDistance();
                return true;
            }
        }
        if(reeling && target==null && distance<1){retract(player);return true;}
        if(reeling)ropeLength=Math.max(1.5,ropeLength-.35);
        // A fixed-length rope only removes outward velocity; tangential movement allows swinging.
        if(target==null && (reeling || distance>ropeLength))pull(player,position().subtract(hand),reeling,1.15);
        if(target!=null && reeling)pull(target,hand.subtract(position()),true,.85);
        return true;
    }
    private static void pull(Entity entity,Vec3 delta,boolean reel,double cap){
        Vec3 velocity=BeeWheelPhysics.pullVelocity(entity.getDeltaMovement(),delta,reel,cap);
        entity.setDeltaMovement(velocity);entity.hurtMarked=true;entity.resetFallDistance();
    }
    @Override public void onRemoval(RemovalReason reason){
        if(!level().isClientSide() && ropeOwner() instanceof ServerPlayer player
                && launcher.getItem() instanceof BeeWheelItem item)
            item.stopTriggeredAnim(player,launcherId,"main",null);
        super.onRemoval(reason);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<BeeWheelEntity>("main",0,test->test.setAndContinue(
                phase()==FLYING?SHOOT:phase()==RETURNING?RETRACT:GRAB)));
    }
}
