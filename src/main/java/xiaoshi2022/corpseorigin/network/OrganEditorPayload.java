package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public record OrganEditorPayload(String json) implements CustomPacketPayload {
    public static final Type<OrganEditorPayload> TYPE = new Type<>(CorpseOrigin.id("organ_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OrganEditorPayload> CODEC = StreamCodec.ofMember(
            (p,b)->b.writeUtf(p.json,8192), b->new OrganEditorPayload(b.readUtf(8192)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public record Catalog(String json) implements CustomPacketPayload {
        public static final Type<Catalog> TYPE = new Type<>(CorpseOrigin.id("organ_catalog"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Catalog> CODEC = StreamCodec.ofMember(
                (p,b)->b.writeUtf(p.json,262144), b->new Catalog(b.readUtf(262144)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Result(boolean success, String message) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(CorpseOrigin.id("organ_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC = StreamCodec.ofMember(
                (p,b)->{b.writeBoolean(p.success);b.writeUtf(p.message,256);}, b->new Result(b.readBoolean(),b.readUtf(256)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
