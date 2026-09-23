package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public record TianGangSwingPayload() implements CustomPacketPayload {
    public static final Type<TianGangSwingPayload> TYPE = new Type<>(CorpseOrigin.id("tian_gang_swing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TianGangSwingPayload> CODEC =
            StreamCodec.ofMember((p, b) -> {}, b -> new TianGangSwingPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
