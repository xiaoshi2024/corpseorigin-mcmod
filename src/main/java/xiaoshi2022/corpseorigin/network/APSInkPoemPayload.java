package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

/**
 * 诗仙剑：某一段落下第 N 句诗
 * @param casterId  施法者
 * @param lineIndex 第几句（0~3）
 */
public record APSInkPoemPayload(UUID casterId, int lineIndex)
        implements CustomPacketPayload {

    public static final Type<APSInkPoemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    CorpseOrigin.MOD_ID, "aps_ink_poem"));

    public static final StreamCodec<FriendlyByteBuf, APSInkPoemPayload> CODEC =
            StreamCodec.ofMember(
                    (payload, buf) -> {
                        buf.writeUUID(payload.casterId());
                        buf.writeVarInt(payload.lineIndex());
                    },
                    buf -> new APSInkPoemPayload(buf.readUUID(), buf.readVarInt())
            );

    @Override
    public Type<APSInkPoemPayload> type() {
        return TYPE;
    }
}