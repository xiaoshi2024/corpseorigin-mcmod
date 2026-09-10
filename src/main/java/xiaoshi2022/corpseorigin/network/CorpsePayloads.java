package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

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

    // ==================== ✅ 玩家尸兄数据同步（用 UUID） ====================

    public record PlayerCorpseSyncS2C(
            UUID playerUuid,       // ✅ 改为 UUID
            boolean isCorpse,
            int corpseType,
            CompoundTag corpseData
    ) implements CustomPacketPayload {
        public static final Type<PlayerCorpseSyncS2C> TYPE = new Type<>(id("player_corpse_sync"));

        public static final StreamCodec<ByteBuf, PlayerCorpseSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                p -> p.playerUuid().toString(),
                ByteBufCodecs.BOOL,
                PlayerCorpseSyncS2C::isCorpse,
                ByteBufCodecs.INT,
                PlayerCorpseSyncS2C::corpseType,
                ByteBufCodecs.COMPOUND_TAG,
                PlayerCorpseSyncS2C::corpseData,
                (uuidStr, isCorpse, corpseType, corpseData) ->
                        new PlayerCorpseSyncS2C(UUID.fromString(uuidStr), isCorpse, corpseType, corpseData)
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}