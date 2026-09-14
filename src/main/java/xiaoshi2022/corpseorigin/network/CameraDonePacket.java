package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

public record CameraDonePacket(UUID targetStateUuid) implements CustomPacketPayload {

    public static final Type<CameraDonePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "camera_done"));

    public static final StreamCodec<FriendlyByteBuf, CameraDonePacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> buf.writeUUID(p.targetStateUuid()),
                    buf -> new CameraDonePacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}