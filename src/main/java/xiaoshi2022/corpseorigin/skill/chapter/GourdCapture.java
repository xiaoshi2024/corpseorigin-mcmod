package xiaoshi2022.corpseorigin.skill.chapter;

import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/** Short-lived capture: move the real target, shrink only its client render, then resolve normal damage. */
public final class GourdCapture {
    public static final AttachmentType<Float> SCALE=AttachmentRegistry.create(xiaoshi2022.corpseorigin.CorpseOrigin.id("gourd_capture_scale"),b->b.initializer(()->1f).syncWith(ByteBufCodecs.FLOAT,AttachmentSyncPredicate.all()));
    private static final Map<UUID,Capture> ACTIVE=new HashMap<>();
    private static final class Capture {
        final ServerPlayer owner;final LivingEntity target;final Vec3 start;final boolean detached;final UUID organ;int age;
        Capture(ServerPlayer p,LivingEntity t){owner=p;target=t;start=t.position();detached=GourdOrganState.detached(p);var g=GourdOrganState.find(p);organ=g==null?null:g.getUUID();}
    }
    private GourdCapture(){}
    public static boolean busy(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
    public static boolean edible(LivingEntity target){
        return GourdBalance.edible(target.getHealth(),target.getMaxHealth(),
                target instanceof net.minecraft.world.entity.npc.villager.AbstractVillager);
    }
    public static boolean begin(ServerPlayer p,LivingEntity target){
        if(!edible(target)||busy(p)||ACTIVE.values().stream().anyMatch(c->c.target==target)||target.isPassenger()||target.isVehicle())return false;
        ACTIVE.put(p.getUUID(),new Capture(p,target));
        target.setDeltaMovement(Vec3.ZERO);target.hurtMarked=true;
        p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.captured"));return true;
    }
    private static void release(Capture c){if(c.target.isAlive())c.target.setAttached(SCALE,1f);c.target.setDeltaMovement(Vec3.ZERO);c.target.hurtMarked=true;}
    private static boolean tick(Capture c){
        var p=c.owner;var t=c.target;var g=GourdOrganState.find(p);
        if(p.isRemoved()||!p.isAlive()||p.isSpectator()||!GourdOrganState.active(p)||GourdOrganState.dead(p)||!t.isAlive()||t.isRemoved()||t.level()!=p.level()||t.isPassenger()||t.isVehicle()||!ChapterCombat.canHit(p,t)||!edible(t)||GourdOrganState.detached(p)!=c.detached)return false;
        if(c.detached&&(g==null||g.level()!=p.level()||!g.getUUID().equals(c.organ)))return false;
        // Approximate the authored snake mouth in model-local coordinates, then retract into the gourd.
        var forward=Vec3.directionFromRotation(0,c.detached?g.getYRot():p.getYRot());
        var base=c.detached?g.position():p.position().add(forward.scale(-.32)).add(0,.55,0);
        var mouth=base.add(0,1.65,0).add(forward.scale(.85));
        if(t.position().distanceToSqr(mouth)>144)return false;
        if(p.level().clip(new ClipContext(t.getBoundingBox().getCenter(),mouth,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)return false;
        c.age++;
        double progress=Math.clamp((c.age-8)/30.0,0,1),smooth=progress*progress*(3-2*progress);
        var intake=mouth.lerp(base.add(0,1.05,0),Math.clamp((c.age-28)/10.0,0,1));
        var destination=c.start.lerp(intake,smooth);
        var motion=destination.subtract(t.position());
        if(motion.lengthSqr()>4)return false;
        if(t instanceof Mob mob)mob.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO);t.move(MoverType.SELF,motion);t.hurtMarked=true;
        if(t.position().distanceToSqr(destination)>.3)return false;
        t.setAttached(SCALE,GourdBalance.captureScale(c.age));
        if(c.age%2==0){for(int i=0;i<6;i++){var at=t.getBoundingBox().getCenter().lerp(mouth,i/5.0);p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,at.x,at.y,at.z,1,.06,.06,.06,.02);}}
        if(c.age<38)return true;
        int gain=GourdBalance.flesh(t.getMaxHealth());
        if(t.hurtServer(p.level(),p.damageSources().playerAttack(p),1000)&&!t.isAlive()){
            int before=BloodReserve.get(p);BloodReserve.add(p,gain);
            p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,mouth.x,mouth.y,mouth.z,18,.15,.15,.15,.02);
            p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.success",BloodReserve.get(p)-before));
        }
        return false;
    }
    public static void register(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{var it=ACTIVE.values().iterator();while(it.hasNext()){var c=it.next();if(!tick(c)){release(c);it.remove();}}});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{var c=ACTIVE.remove(h.player.getUUID());if(c!=null)release(c);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{ACTIVE.values().forEach(GourdCapture::release);ACTIVE.clear();});
    }
}
