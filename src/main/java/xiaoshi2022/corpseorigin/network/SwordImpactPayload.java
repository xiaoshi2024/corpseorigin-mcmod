package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/** A single bounded packet creates an entire client-side flower of sword strokes. */
public record SwordImpactPayload(Vec3 center,Vec3 direction,int tier,int kind,int target,int caster,long seed) implements CustomPacketPayload {
    public static final Type<SwordImpactPayload> TYPE=new Type<>(CorpseOrigin.id("sword_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SwordImpactPayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeDouble(p.center.x);b.writeDouble(p.center.y);b.writeDouble(p.center.z);b.writeDouble(p.direction.x);b.writeDouble(p.direction.y);b.writeDouble(p.direction.z);b.writeVarInt(p.tier);b.writeVarInt(p.kind);b.writeInt(p.target);b.writeInt(p.caster);b.writeLong(p.seed);},
            b->new SwordImpactPayload(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt(),b.readVarInt(),b.readInt(),b.readInt(),b.readLong()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
