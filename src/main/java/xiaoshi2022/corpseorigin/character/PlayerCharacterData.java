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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerCharacterData extends SavedData {

    // 先定义ENTRY_CODEC
    private static final Codec<PlayerEntry> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("character", MortalCharacter.ID).forGetter(e -> e.characterId),
            Codec.list(Codec.STRING)
                    .optionalFieldOf("learned_skills", List.of())
                    .forGetter(e -> List.copyOf(e.learnedSkills)),
            Codec.INT.optionalFieldOf("earned_points", 0).forGetter(e -> e.earnedPoints),
            Codec.INT.optionalFieldOf("available_points", 0).forGetter(e -> e.availablePoints)
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
        public Set<String> learnedSkills = new LinkedHashSet<>();
        public int earnedPoints = 0;
        public int availablePoints = 0;

        public PlayerEntry() {
        }

        private PlayerEntry(String characterId, List<String> learnedSkills,
                            int earnedPoints, int availablePoints) {
            this.characterId = characterId;
            this.learnedSkills = new LinkedHashSet<>(learnedSkills);
            this.earnedPoints = earnedPoints;
            this.availablePoints = availablePoints;
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

    // ==================== 技能学习相关 ====================

    public Set<String> getLearnedSkills(UUID uuid) {
        return getEntry(uuid).learnedSkills;
    }

    public boolean hasLearned(UUID uuid, String skillPath) {
        return getEntry(uuid).learnedSkills.contains(skillPath);
    }

    public void learnSkill(UUID uuid, String skillPath) {
        if (getEntry(uuid).learnedSkills.add(skillPath)) {
            setDirty();
        }
    }

    // ==================== 进化点相关 ====================

    public int getEarnedPoints(UUID uuid) {
        return getEntry(uuid).earnedPoints;
    }

    public int getAvailablePoints(UUID uuid) {
        return getEntry(uuid).availablePoints;
    }

    public void addEarnedPoints(UUID uuid, int amount) {
        PlayerEntry entry = getEntry(uuid);
        entry.earnedPoints += amount;
        entry.availablePoints += amount;
        setDirty();
    }

    public boolean spendPoints(UUID uuid, int amount) {
        PlayerEntry entry = getEntry(uuid);
        if (entry.availablePoints < amount) {
            return false;
        }
        entry.availablePoints -= amount;
        setDirty();
        return true;
    }

    public void setPoints(UUID uuid, int earned, int available) {
        PlayerEntry entry = getEntry(uuid);
        entry.earnedPoints = earned;
        entry.availablePoints = available;
        setDirty();
    }

    public void clearLearnedSkills(UUID uuid) {
        getEntry(uuid).learnedSkills.clear();
        setDirty();
    }
}