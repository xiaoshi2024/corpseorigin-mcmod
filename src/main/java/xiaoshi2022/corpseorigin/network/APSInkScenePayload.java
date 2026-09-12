package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

public record APSInkScenePayload(
        UUID casterId,
        double x, double y, double z,
        double riverDirX, double riverDirZ,
        boolean open
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<APSInkScenePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "aps_ink_scene"));

    public static final StreamCodec<FriendlyByteBuf, APSInkScenePayload> CODEC =
            StreamCodec.ofMember(
                    (payload, buf) -> {
                        buf.writeUUID(payload.casterId());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeDouble(payload.riverDirX());
                        buf.writeDouble(payload.riverDirZ());
                        buf.writeBoolean(payload.open());
                    },
                    buf -> new APSInkScenePayload(
                            buf.readUUID(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readBoolean())
            );

    @Override
    public Type<APSInkScenePayload> type() {
        return TYPE;
    }
}