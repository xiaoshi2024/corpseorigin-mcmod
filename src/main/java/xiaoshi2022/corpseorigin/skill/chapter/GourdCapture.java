package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived capture: move the real target, shrink only its client render, then resolve normal damage. */
public final class GourdCapture {
    public static final AttachmentType<Float> SCALE=AttachmentRegistry.create(xiaoshi2022.corpseorigin.CorpseOrigin.id("gourd_capture_scale"),b->b.initializer(()->1f).syncWith(ByteBufCodecs.FLOAT,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> ANCHOR=AttachmentRegistry.create(xiaoshi2022.corpseorigin.CorpseOrigin.id("gourd_capture_anchor"),b->b.initializer(()->-1).syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    private static final Map<UUID,Capture> ACTIVE=new HashMap<>();
    private static final class Capture {
        boolean automatic;
        final ServerPlayer owner;final LivingEntity target;final Vec3 start;final boolean detached;final UUID organ;int age=-14;
        Capture(ServerPlayer p,LivingEntity t){owner=p;target=t;start=t.position();detached=GourdOrganState.detached(p);var g=GourdOrganState.find(p);organ=g==null?null:g.getUUID();t.setAttached(ANCHOR,detached&&g!=null?g.getId():p.getId());if(t instanceof Mob m)m.getNavigation().stop();}
    }
    private GourdCapture(){}
    public static boolean busy(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
    public static void cancel(ServerPlayer p){var capture=ACTIVE.remove(p.getUUID());if(capture!=null)release(capture);}
    public static boolean beginPet(ServerPlayer p,xiaoshi2022.corpseorigin.entity.GourdOrganEntity g,LivingEntity target){
        if(!GourdOrganState.isCurrent(p,g)||g.staying()||!g.petPrey(p,target)||GourdOrganState.windingUp(p)||!begin(p,target))return false;
        ACTIVE.get(p.getUUID()).automatic=true;
        GourdOrganState.play(p,3,GourdBalance.duration(3));
        return true;
    }
    public static boolean edible(LivingEntity target){
        if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon
                && dragon.getPhaseManager().getCurrentPhase().getPhase() == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING) return false;
        if (boss(target)) return GourdTrait.bossEdible(target.getHealth(), target.getMaxHealth());
        return GourdBalance.edible(target.getHealth(),target.getMaxHealth(),
                target instanceof net.minecraft.world.entity.npc.villager.AbstractVillager);
    }
    public static boolean begin(ServerPlayer p,LivingEntity target){
        if(!GourdOrganState.active(p)||!validPrey(p,target)||!edible(target)||busy(p)||ACTIVE.values().stream().anyMatch(c->c.target==target)||target.isPassenger()||target.isVehicle())return false;
        GourdInheritance.reveal(p);
        ACTIVE.put(p.getUUID(),new Capture(p,target));
        target.setDeltaMovement(Vec3.ZERO);target.hurtMarked=true;
        p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.captured"));return true;
    }
    public static LivingEntity target(Entity entity) {
        return entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part ? part.parentMob
                : entity instanceof LivingEntity living ? living : null;
    }
    public static boolean validPrey(ServerPlayer p, LivingEntity target) {
        return target != null && !(target instanceof net.minecraft.world.entity.player.Player)
                && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand)
                && !(target instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame())
                && !(target instanceof xiaoshi2022.corpseorigin.entity.OwnerBound)
                && ChapterCombat.canHit(p, target);
    }
    private static boolean boss(LivingEntity target) {
        return target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || target instanceof net.minecraft.world.entity.boss.wither.WitherBoss;
    }
    private static void release(Capture c){c.target.setAttached(SCALE,1f);c.target.setAttached(ANCHOR,-1);c.target.setDeltaMovement(Vec3.ZERO);c.target.hurtMarked=true;}
    private static boolean tick(Capture c){
        var p=c.owner;var t=c.target;var g=GourdOrganState.find(p);
        if(p.isRemoved()||!p.isAlive()||p.isSpectator()||!GourdOrganState.active(p)||GourdOrganState.dead(p)||!t.isAlive()||t.isRemoved()||t.level()!=p.level()||t.isPassenger()||t.isVehicle()||!ChapterCombat.canHit(p,t)||GourdOrganState.detached(p)!=c.detached)return false;
        if(c.detached&&(g==null||g.level()!=p.level()||!g.getUUID().equals(c.organ)))return false;
        if(c.automatic&&(g.staying()||g.distanceToSqr(p)>576))return false;
        // Server collision proxy only; clients draw the intake at the animated snake_head.
        var forward=Vec3.directionFromRotation(0,c.detached?g.getYRot():p.getYRot());
        var base=c.detached?g.position():p.position().add(forward.scale(-.32)).add(0,.55,0);
        var mouth=base.add(0,1.65,0).add(forward.scale(.85));
        c.age++;
        double progress=Math.clamp((c.age-8)/30.0,0,1),smooth=progress*progress*(3-2*progress);
        var intake=mouth.lerp(base.add(0,1.05,0),Math.clamp((c.age-28)/10.0,0,1));
        var destination=c.start.lerp(intake,smooth);
        var motion=destination.subtract(t.position());
        if(motion.lengthSqr()>4)motion=motion.normalize().scale(2);
        if(t instanceof Mob mob)mob.getNavigation().stop();
        // Keep boss fight position/portal logic intact; only drain their weakened essence.
        if (!boss(t)) { t.setDeltaMovement(Vec3.ZERO);t.move(MoverType.SELF,motion);t.hurtMarked=true; }
        // Once caught, movement, turning and collision do not re-run acquisition or release it.
        t.setAttached(SCALE,GourdBalance.captureScale(c.age));
        if(c.age<38)return true;
        int gain=GourdBalance.flesh(t.getMaxHealth());
        // Percentage eligibility also admits high-health targets; scale the finishing hit accordingly.
        float finishDamage=(float)Math.min(Float.MAX_VALUE,Math.max(1000,((double)t.getHealth()+t.getAbsorptionAmount())*100));
        var source = c.automatic ? p.damageSources().mobAttack(g) : p.damageSources().playerAttack(p);
        boolean hurt = t instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon
                ? dragon.hurt(p.level(), dragon.head, source, finishDamage) : t.hurtServer(p.level(), source, finishDamage);
        boolean finished = !t.isAlive() || t instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon
                && dragon.getPhaseManager().getCurrentPhase().getPhase() == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING;
        if(hurt && finished){
            int before=BloodReserve.get(p);
            if(c.automatic)g.storeFlesh(GourdBalance.petFlesh(t.getMaxHealth()));else BloodReserve.add(p,gain);
            QiEffects.burst((net.minecraft.server.level.ServerLevel)p.level(),mouth.x,mouth.y,mouth.z,0x9aa4b0,18,.15);
            p.sendOverlayMessage(c.automatic?Component.translatable("message.corpseorigin.gourd.pet_stored",g.storedFlesh(),GourdBalance.PET_CAPACITY):Component.translatable("skill.corpseorigin.gourd_devour.success",BloodReserve.get(p)-before));
            GourdInheritance.onDevoured(p, t, c.automatic);
        }
        return false;
    }
    public static void register(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{var it=ACTIVE.values().iterator();while(it.hasNext()){var c=it.next();if(!tick(c)){release(c);it.remove();}}});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{var c=ACTIVE.remove(h.player.getUUID());if(c!=null)release(c);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{ACTIVE.values().forEach(GourdCapture::release);ACTIVE.clear();});
    }
}
