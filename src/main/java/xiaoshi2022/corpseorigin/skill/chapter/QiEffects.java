package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.network.QiAuraPayload;

/** Visual-only qi; short leases refresh from the actual skill lifecycle. */
public final class QiEffects {
    private QiEffects() {}
    public static void aura(Entity entity,String channel,int color,float radius,int ticks){
        if(entity.level() instanceof ServerLevel level)
            send(level,new QiAuraPayload(entity.getId(),channel,entity.getBoundingBox().getCenter(),color,radius,ticks));
    }
    public static void cloud(ServerLevel level,Vec3 center,int color,float radius,int ticks){
        send(level,new QiAuraPayload(-1,"cloud",center,color,radius,ticks));
    }
    private static void send(ServerLevel level,QiAuraPayload payload){
        for(var player:level.players())if(player.distanceToSqr(payload.center())<9216)
            ServerPlayNetworking.send(player,payload);
    }
}
