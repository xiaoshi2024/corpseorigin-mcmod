package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class CorpsePayloads {

    private CorpsePayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, path);
    }

    // ==================== 角色选择 ====================

    public record SelectCharacterC2S(String characterId) implements CustomPacketPayload {
        public static final Type<SelectCharacterC2S> TYPE = new Type<>(id("select_character"));
        public static final StreamCodec<ByteBuf, SelectCharacterC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.characterId()),
                        buf -> new SelectCharacterC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CharacterSyncS2C(String characterId) implements CustomPacketPayload {
        public static final Type<CharacterSyncS2C> TYPE = new Type<>(id("character_sync"));
        public static final StreamCodec<ByteBuf, CharacterSyncS2C> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.characterId()),
                        buf -> new CharacterSyncS2C(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ❌ 删除 ZbSkinUpdateC2S，使用独立的 ZbSkinUpdatePacket
}