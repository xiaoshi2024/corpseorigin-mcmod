package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public record QiAuraPayload(int anchor,String channel,Vec3 center,int color,float radius,int ticks) implements CustomPacketPayload {
    public static final Type<QiAuraPayload> TYPE=new Type<>(CorpseOrigin.id("qi_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf,QiAuraPayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeInt(p.anchor);b.writeUtf(p.channel,64);b.writeDouble(p.center.x);b.writeDouble(p.center.y);b.writeDouble(p.center.z);b.writeInt(p.color);b.writeFloat(p.radius);b.writeVarInt(p.ticks);},
            b->new QiAuraPayload(b.readInt(),b.readUtf(64),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readInt(),b.readFloat(),b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
