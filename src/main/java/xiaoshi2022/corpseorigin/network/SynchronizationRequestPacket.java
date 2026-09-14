package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

public record SynchronizationRequestPacket(UUID targetStateUuid) implements CustomPacketPayload {

    public static final Type<SynchronizationRequestPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "sync_request"));

    public static final StreamCodec<FriendlyByteBuf, SynchronizationRequestPacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> buf.writeUUID(p.targetStateUuid()),
                    buf -> new SynchronizationRequestPacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}