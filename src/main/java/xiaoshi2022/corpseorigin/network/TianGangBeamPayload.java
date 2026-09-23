package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import java.util.UUID;

/** Server-only beam target updates; zero ticks explicitly cancels the visual. */
public record TianGangBeamPayload(UUID owner, UUID cast, net.minecraft.world.InteractionHand hand, Vec3 end, double length, boolean firing, int ticks) implements CustomPacketPayload {
    public static final Type<TianGangBeamPayload> TYPE = new Type<>(CorpseOrigin.id("tian_gang_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TianGangBeamPayload> CODEC = StreamCodec.ofMember(
            (p, b) -> { b.writeUUID(p.owner); b.writeUUID(p.cast); b.writeEnum(p.hand); b.writeDouble(p.end.x); b.writeDouble(p.end.y); b.writeDouble(p.end.z);
                b.writeDouble(p.length); b.writeBoolean(p.firing); b.writeVarInt(p.ticks); },
            b -> new TianGangBeamPayload(b.readUUID(), b.readUUID(), b.readEnum(net.minecraft.world.InteractionHand.class), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                    b.readDouble(), b.readBoolean(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
