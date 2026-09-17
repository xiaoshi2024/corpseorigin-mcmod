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

    public record InfectionSyncS2C(int infection) implements CustomPacketPayload {
        public static final Type<InfectionSyncS2C> TYPE = new Type<>(id("infection_sync"));

        public static final StreamCodec<ByteBuf, InfectionSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT,
                InfectionSyncS2C::infection,
                InfectionSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TempRedEyeSyncS2C(UUID playerUuid, int durationTicks) implements CustomPacketPayload {
        public static final Type<TempRedEyeSyncS2C> TYPE = new Type<>(id("temp_red_eye_sync"));
        public static final StreamCodec<ByteBuf, TempRedEyeSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, TempRedEyeSyncS2C::durationTicks,
                (s, t) -> new TempRedEyeSyncS2C(UUID.fromString(s), t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * 天线宝宝尸兄的「吸食」状态（S2C）。
     * <p>
     * 驱动穿在身上的天线宝宝盔甲播 {@code absorb} 动画，并让触手转向
     * {@code targetEntityId} 这具目标（动画里的 {@code query.target_*_rotation}）；
     * {@code durationTicks <= 0} 表示立刻结束（松手 / 被打断），客户端收到后直接清掉状态。
     */
    public record AntennaSuckSyncS2C(UUID playerUuid, int targetEntityId, int durationTicks)
            implements CustomPacketPayload {
        public static final Type<AntennaSuckSyncS2C> TYPE = new Type<>(id("antenna_suck_sync"));
        public static final StreamCodec<ByteBuf, AntennaSuckSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, p -> p.playerUuid().toString(),
                ByteBufCodecs.INT, AntennaSuckSyncS2C::targetEntityId,
                ByteBufCodecs.INT, AntennaSuckSyncS2C::durationTicks,
                (s, targetId, t) -> new AntennaSuckSyncS2C(UUID.fromString(s), targetId, t));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
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

    // ==================== ✅ 学习技能（C2S） ====================

    public record LearnSkillC2S(String skillPath) implements CustomPacketPayload {
        public static final Type<LearnSkillC2S> TYPE = new Type<>(id("learn_skill"));

        public static final StreamCodec<ByteBuf, LearnSkillC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.skillPath()),
                        buf -> new LearnSkillC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 技能激活（C2S） ====================

    public record ActivateSkillC2S(String skillPath) implements CustomPacketPayload {
        public static final Type<ActivateSkillC2S> TYPE = new Type<>(id("activate_skill"));

        public static final StreamCodec<ByteBuf, ActivateSkillC2S> CODEC =
                CustomPacketPayload.codec(
                        (payload, buf) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.skillPath()),
                        buf -> new ActivateSkillC2S(ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 进化/技能同步（S2C） ====================

    public record EvolutionSyncS2C(int earnedPoints, int availablePoints, int kills, byte[] learnedSkills) implements CustomPacketPayload {
        public static final Type<EvolutionSyncS2C> TYPE = new Type<>(id("evolution_sync"));

        public static final StreamCodec<ByteBuf, EvolutionSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT,
                EvolutionSyncS2C::earnedPoints,
                ByteBufCodecs.INT,
                EvolutionSyncS2C::availablePoints,
                ByteBufCodecs.INT,
                EvolutionSyncS2C::kills,
                ByteBufCodecs.BYTE_ARRAY,
                EvolutionSyncS2C::learnedSkills,
                EvolutionSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================== ✅ 技能冷却同步（S2C） ====================

    public record CooldownSyncS2C(String skillPath, int ticks) implements CustomPacketPayload {
        public static final Type<CooldownSyncS2C> TYPE = new Type<>(id("cooldown_sync"));

        public static final StreamCodec<ByteBuf, CooldownSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                CooldownSyncS2C::skillPath,
                ByteBufCodecs.INT,
                CooldownSyncS2C::ticks,
                CooldownSyncS2C::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}