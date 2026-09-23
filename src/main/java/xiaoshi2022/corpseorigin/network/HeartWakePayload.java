package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public record HeartWakePayload() implements CustomPacketPayload {
    public static final Type<HeartWakePayload> TYPE = new Type<>(CorpseOrigin.id("heart_wake"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeartWakePayload> CODEC =
            StreamCodec.unit(new HeartWakePayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
