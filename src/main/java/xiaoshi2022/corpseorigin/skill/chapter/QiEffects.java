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
    /**
     * 定点气团，直接顶替一次 {@code level.sendParticles(...)}：按原版粒子的「数量 + 扩散」
     * 换算成气团的半径与时长，整体只发一朵，不再逐个粒子。
     * <p>
     * 换算经验值：扩散是粒子的半边长，1 颗小火花的 spread≈0 → 半径 0.5、8 tick；
     * 30 颗 spread≈2 的爆炸 → 半径 5 上下、23 tick。半径上限 8（渲染端 16 才截断）。
     */
    public static void burst(ServerLevel level,double x,double y,double z,int color,int count,double spread){
        float radius=(float)Math.min(8.0,Math.max(.5,.45+spread*2.2+count*.035));
        cloud(level,new Vec3(x,y,z),color,radius,8+Math.min(24,count/2));
    }
    private static void send(ServerLevel level,QiAuraPayload payload){
        for(var player:level.players())if(player.distanceToSqr(payload.center())<9216)
            ServerPlayNetworking.send(player,payload);
    }
}
