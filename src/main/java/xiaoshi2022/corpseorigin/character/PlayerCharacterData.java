package xiaoshi2022.corpseorigin.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.*;

public class PlayerCharacterData extends SavedData {

    // 先定义ENTRY_CODEC
    private static final Codec<PlayerEntry> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("character", MortalCharacter.ID).forGetter(e -> e.characterId)
    ).apply(inst, PlayerEntry::new));

    private static final Codec<PlayerCharacterData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, ENTRY_CODEC)
                    .xmap(PlayerCharacterData::uuidMapFrom, PlayerCharacterData::uuidMapTo)
                    .optionalFieldOf("players", Map.of())
                    .forGetter(d -> Map.copyOf(d.players))
    ).apply(inst, PlayerCharacterData::new));

    public static final SavedDataType<PlayerCharacterData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "character_data"),
            PlayerCharacterData::new,
            CODEC,
            null
    );

    private final Map<UUID, PlayerEntry> players = new HashMap<>();

    public PlayerCharacterData() {
    }

    private PlayerCharacterData(Map<UUID, PlayerEntry> players) {
        this.players.putAll(players);
    }

    private static Map<UUID, PlayerEntry> uuidMapFrom(Map<String, PlayerEntry> raw) {
        Map<UUID, PlayerEntry> result = new HashMap<>();
        raw.forEach((key, entry) -> {
            try {
                result.put(UUID.fromString(key), entry);
            } catch (IllegalArgumentException ignored) {
            }
        });
        return result;
    }

    private static Map<String, PlayerEntry> uuidMapTo(Map<UUID, PlayerEntry> map) {
        Map<String, PlayerEntry> result = new HashMap<>();
        map.forEach((uuid, entry) -> result.put(uuid.toString(), entry));
        return result;
    }

    public static PlayerCharacterData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static PlayerCharacterData get(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return get(serverLevel.getServer());
        }
        return new PlayerCharacterData();
    }

    public PlayerEntry getEntry(UUID uuid) {
        return players.computeIfAbsent(uuid, k -> new PlayerEntry());
    }

    // ==================== 数据条目 ====================

    public static class PlayerEntry {
        public String characterId = MortalCharacter.ID;

        public PlayerEntry() {
        }

        private PlayerEntry(String characterId) {
            this.characterId = characterId;
        }
    }

    // ==================== 角色相关 ====================

    public String getCharacterId(UUID uuid) {
        return getEntry(uuid).characterId;
    }

    public void setCharacterId(UUID uuid, String characterId) {
        getEntry(uuid).characterId = characterId;
        setDirty();
    }
}