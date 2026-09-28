package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.registry.ModEffects;

public final class ThermalSurvey {
    private ThermalSurvey(){}
    public record Result(int explored,int loaded,int corpses,int corpsePlayers,int incubating){public int unloaded(){return explored-loaded;}}
    private static final java.util.Map<java.util.UUID,String> HUD_DIMENSIONS=new java.util.HashMap<>();
    public static boolean carriesScanner(Player p){
        for(int i=0;i<p.getInventory().getContainerSize();i++)
            if(p.getInventory().getItem(i).is(xiaoshi2022.corpseorigin.registry.ModItems.ZISHU_THERMAL_SCANNER))return true;
        return p.getOffhandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.ZISHU_THERMAL_SCANNER);
    }
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                xiaoshi2022.corpseorigin.network.ThermalSurveyPayload.TYPE,xiaoshi2022.corpseorigin.network.ThermalSurveyPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->HUD_DIMENSIONS.remove(h.player.getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->HUD_DIMENSIONS.clear());
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers()){
            if(!p.isSpectator())ThermalSurveyData.get(p).visit(p);
            String dimension=p.level().dimension().identifier().toString();
            boolean active=p.isAlive()&&!p.isSpectator()&&carriesScanner(p);
            if(!active){
                if(HUD_DIMENSIONS.remove(p.getUUID())!=null)
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new xiaoshi2022.corpseorigin.network.ThermalSurveyPayload(dimension,false,new Result(0,0,0,0,0)));
            }else if(!dimension.equals(HUD_DIMENSIONS.get(p.getUUID()))
                    ||Math.floorMod(p.tickCount+p.getId(),100)==0){
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new xiaoshi2022.corpseorigin.network.ThermalSurveyPayload(dimension,true,scan(p)));
                HUD_DIMENSIONS.put(p.getUUID(),dimension);
            }
        }});
    }
    public static boolean isCorpse(LivingEntity e){
        if (e instanceof Player p) return PlayerCorpseComponent.isCorpse(p);
        if (e instanceof CloneAvatarEntity clone) return clone.isCorpseClone() || clone.isCorpseKingBody();
        return ZombieKin.isZombieKin(e);
    }
    public static Result scan(ServerPlayer p){
        var data=ThermalSurveyData.get(p);data.visit(p);var chunks=data.chunks(p);int loaded=0,corpses=0,players=0,incubating=0;
        for(long key:chunks)if(p.level().getChunkSource().hasChunk(ChunkPos.getX(key),ChunkPos.getZ(key)))loaded++;
        for(var entity:p.level().getAllEntities()){
            if(!(entity instanceof LivingEntity e)||!e.isAlive()||e.isSpectator()||!chunks.contains(e.chunkPosition().pack())
                    ||!p.level().getChunkSource().hasChunk(e.chunkPosition().x(),e.chunkPosition().z()))continue;
            if(isCorpse(e)){if(e instanceof Player)players++;else corpses++;}
            else if(e.hasEffect(ModEffects.QIANS))incubating++;
        }
        return new Result(chunks.size(),loaded,corpses,players,incubating);
    }
    public static void report(ServerPlayer p){var r=scan(p);p.sendSystemMessage(Component.translatable("thermal.corpseorigin.report",p.level().dimension().identifier().toString(),r.explored(),r.loaded(),r.unloaded(),r.corpses(),r.corpsePlayers(),r.incubating()));}
}
