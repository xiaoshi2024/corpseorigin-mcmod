package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public record JuQueBeamPacket() implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            CorpseOrigin.MOD_ID, "juque_beam");

    public static final Type<JuQueBeamPacket> TYPE = new Type<>(ID);

    // ✅ 空包用 StreamCodec.unit
    public static final StreamCodec<ByteBuf, JuQueBeamPacket> CODEC =
            StreamCodec.unit(new JuQueBeamPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}