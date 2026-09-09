package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
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

    // ==================== ✅ 玩家尸兄数据同步 ====================

    public record PlayerCorpseSyncS2C(
            int playerId,
            boolean isCorpse,
            int corpseType,
            CompoundTag corpseData
    ) implements CustomPacketPayload {
        public static final Type<PlayerCorpseSyncS2C> TYPE = new Type<>(id("player_corpse_sync"));

        public static final StreamCodec<ByteBuf, PlayerCorpseSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT,
                PlayerCorpseSyncS2C::playerId,
                ByteBufCodecs.BOOL,  // ✅ 修复：使用 BOOL 而不是 BOOLEAN
                PlayerCorpseSyncS2C::isCorpse,
                ByteBufCodecs.INT,
                PlayerCorpseSyncS2C::corpseType,
                ByteBufCodecs.COMPOUND_TAG,
                PlayerCorpseSyncS2C::corpseData,
                PlayerCorpseSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ❌ 删除 ZbSkinUpdateC2S，使用独立的 ZbSkinUpdatePacket
}