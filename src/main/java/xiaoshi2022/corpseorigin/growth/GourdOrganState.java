package xiaoshi2022.corpseorigin.growth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.entity.GourdOrganEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Body attachment owns form/health; detached creatures have stable persisted identities. */
public final class GourdOrganState {
    private GourdOrganState(){}
    private record Pending(long at, int remaining, int interval, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,java.util.function.Consumer<ServerPlayer> action){}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    public static boolean active(Player p){return "xiaojingang".equals(p.level().isClientSide()
            ? p.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE)
            : xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(p));}
    public static boolean detached(Player p){return p.getAttachedOrCreate(SurvivalGrowth.BODY).getBooleanOr("gourd_detached",false);}
    public static boolean dead(Player p){return p.getAttachedOrCreate(SurvivalGrowth.BODY).getBooleanOr("gourd_dead",false);}
    public static boolean isCurrent(Player p,GourdOrganEntity e){return e.getUUID().toString().equals(p.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr("gourd_entity",""));}
    public static int form(Player p){var b=p.getAttachedOrCreate(SurvivalGrowth.BODY);return p.level().getGameTime()<b.getLongOr("gourd_until",0)?b.getIntOr("gourd_form",0):0;}
    public static String clip(int form,boolean detached){return switch(form){case 1->"eyez";case 2->"hand";case 3->"snake";case 4->"snake_whater";case 5->"snake_fire";case 6->"power";default->detached?"snake":"idle";};}
    public static String color(int form){return switch(form){case 2,6->"gold";case 4->"purple";case 5->"red";default->"dark";};}
    public static boolean windingUp(Player p){return PENDING.containsKey(p.getUUID()) || p instanceof ServerPlayer sp && xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.busy(sp);}
    public static void schedule(ServerPlayer p,java.util.function.Consumer<ServerPlayer> action){schedule(p,1,10,action);}
    public static void schedule(ServerPlayer p,int pulses,int interval,java.util.function.Consumer<ServerPlayer> action){PENDING.put(p.getUUID(),new Pending(p.level().getGameTime()+14,pulses,interval,p.level().dimension(),action));}
    public static void play(ServerPlayer p,int form,int ticks){var b=p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();b.putInt("gourd_form",form);b.putLong("gourd_until",p.level().getGameTime()+ticks);p.setAttached(SurvivalGrowth.BODY,b);var e=find(p);if(e!=null)e.play(form,ticks);}
    public static GourdOrganEntity find(ServerPlayer p){String id=p.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr("gourd_entity","");if(id.isEmpty())return null;try{UUID uuid=UUID.fromString(id);for(var l:p.level().getServer().getAllLevels())if(l.getEntity(uuid) instanceof GourdOrganEntity g && g.isAlive() && g.isOwnedBy(p))return g;}catch(IllegalArgumentException ignored){}return null;}
    public static void saveHealth(ServerPlayer p,float value){var b=p.getAttachedOrCreate(SurvivalGrowth.BODY);if(b.getFloatOr("gourd_health",40)==value)return;b=b.copy();b.putFloat("gourd_health",value);p.setAttached(SurvivalGrowth.BODY,b);}
    public static void killed(ServerPlayer p){var b=p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();b.putBoolean("gourd_dead",true);b.putBoolean("gourd_detached",false);b.remove("gourd_entity");b.putFloat("gourd_health",0);b.remove("gourd_stored_flesh");p.setAttached(SurvivalGrowth.BODY,b);PENDING.remove(p.getUUID());p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd.dead"));}
    public static void toggle(ServerPlayer p){
        if(windingUp(p)){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.busy"));return;}
        var b=p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        if(dead(p)){
            if(!p.isCreative() && !xiaoshi2022.corpseorigin.skill.SkillResources.pay(p,new xiaoshi2022.corpseorigin.skill.SkillResourceRules.Cost(0,100)))return;
            b.putBoolean("gourd_dead",false);b.putFloat("gourd_health",40);b.putBoolean("gourd_detached",false);p.setAttached(SurvivalGrowth.BODY,b);p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd.regrown"));return;
        }
        if(detached(p)){var e=find(p);if(e==null && !b.getStringOr("gourd_entity","").isEmpty()){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd.away"));return;}if(e!=null){b.putFloat("gourd_health",e.getHealth());
            int gained=xiaoshi2022.corpseorigin.skill.chapter.GourdBalance.transfer(e.storedFlesh(),xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.get(p));
            xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.add(p,gained);
            b.putInt("gourd_stored_flesh",e.storedFlesh()-gained);
            p.sendSystemMessage(Component.translatable("message.corpseorigin.gourd.transferred",gained,e.storedFlesh()-gained));e.restoreStoredFlesh(0);e.discard();}b.remove("gourd_entity");b.putBoolean("gourd_detached",false);}
        else{
            var e=xiaoshi2022.corpseorigin.registry.ModEntities.ZBR_GOURD.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);if(e==null)return;
            e.setOwner(p);e.restoreStoredFlesh(b.getIntOr("gourd_stored_flesh",0));e.setHealth(Math.clamp(b.getFloatOr("gourd_health",40),1,40));
            var desired=p.position().add(net.minecraft.world.phys.Vec3.directionFromRotation(0,p.getYRot()).scale(2));
            e.setPos(desired);if(!p.level().noCollision(e)){e.setPos(p.position());if(!p.level().noCollision(e)){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd.no_room"));return;}}
            if(!p.level().addFreshEntity(e))return;b.remove("gourd_stored_flesh");b.putString("gourd_entity",e.getUUID().toString());b.putBoolean("gourd_detached",true);
        }
        p.setAttached(SurvivalGrowth.BODY,b);p.sendOverlayMessage(Component.translatable(detached(p)?"skill.corpseorigin.gourd_link.detached":"skill.corpseorigin.gourd_link.attached"));
    }
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
        var pending=PENDING.get(p.getUUID());if(pending!=null){if(!active(p)||!p.isAlive()||p.isSpectator()||dead(p)||p.level().dimension()!=pending.dimension)PENDING.remove(p.getUUID());else if(p.level().getGameTime()>=pending.at){if(pending.remaining>1)PENDING.put(p.getUUID(),new Pending(pending.at+pending.interval,pending.remaining-1,pending.interval,pending.dimension,pending.action));else PENDING.remove(p.getUUID());pending.action.accept(p);}}
        if(!active(p) && detached(p)){var e=find(p);if(e!=null)e.discard();var b=p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();b.putBoolean("gourd_detached",false);b.remove("gourd_entity");p.setAttached(SurvivalGrowth.BODY,b);}
    }});net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->PENDING.clear());net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->PENDING.remove(h.player.getUUID()));}
}
