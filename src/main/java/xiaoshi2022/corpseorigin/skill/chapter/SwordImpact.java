package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.network.SwordImpactPayload;

public final class SwordImpact {
    private SwordImpact() {}
    public static void send(ServerLevel level,Vec3 at,Vec3 direction,int tier,int kind,int target,int caster) {
        var packet=new SwordImpactPayload(at,direction.normalize(),SwordQiRules.tier(tier),kind,target,caster,level.getRandom().nextLong());
        double reach=kind==1?384:kind==2?192:128;
        for(ServerPlayer p:level.players())if(p.distanceToSqr(at)<reach*reach || p.getId()==caster)ServerPlayNetworking.send(p,packet);
        if(kind!=2)level.playSound(null,at.x,at.y,at.z,kind==1?SoundEvents.LIGHTNING_BOLT_THUNDER:SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,kind==1?3:1.6f,kind==1?.65f:.7f);
    }
}
