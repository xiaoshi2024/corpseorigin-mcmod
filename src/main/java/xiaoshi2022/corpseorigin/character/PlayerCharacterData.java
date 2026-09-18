package xiaoshi2022.corpseorigin.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
            Codec.STRING.optionalFieldOf("character", MortalCharacter.ID).forGetter(e -> e.characterId),
            Codec.list(Codec.STRING)
                    .optionalFieldOf("learned_skills", List.of())
                    .forGetter(e -> List.copyOf(e.learnedSkills)),
            Codec.INT.optionalFieldOf("earned_points", 0).forGetter(e -> e.earnedPoints),
            Codec.INT.optionalFieldOf("available_points", 0).forGetter(e -> e.availablePoints),
            Codec.BOOL.optionalFieldOf("starter_book", false).forGetter(e -> e.starterBookGiven)
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
        /** 是否已经领过"生存开局"送的那本角色选择书（只发一次，防重复登录白嫖） */
        public boolean starterBookGiven = false;

        public PlayerEntry() {
        }

        private PlayerEntry(String characterId, List<String> learnedSkills,
                            int earnedPoints, int availablePoints, boolean starterBookGiven) {
            this.characterId = characterId;
            for (String skill : learnedSkills) {
                this.learnedSkills.add(normalizeSkillId(skill));
            }
            this.earnedPoints = earnedPoints;
            this.availablePoints = availablePoints;
            this.starterBookGiven = starterBookGiven;
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

    // ==================== 开局角色书 ====================

    /** 是否已经领过"生存开局"送的那本统一角色书 */
    public boolean hasReceivedStarterBook(UUID uuid) {
        return getEntry(uuid).starterBookGiven;
    }

    /**
     * 标记开局角色书已发。
     * <p>
     * <b>发的同一刻就写</b>，而不是等玩家用掉才写 —— 否则玩家每次登录都会再收到一本。
     */
    public void markStarterBookReceived(UUID uuid) {
        getEntry(uuid).starterBookGiven = true;
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

    /** ★ 导出某个玩家 UUID 的条目为 NBT */
    public CompoundTag writeNbt(UUID uuid) {
        PlayerEntry entry = getEntry(uuid);
        CompoundTag tag = new CompoundTag();
        tag.putString("CharacterId", entry.characterId == null ? MortalCharacter.ID : entry.characterId);

        ListTag skills = new ListTag();
        for (String s : entry.learnedSkills) skills.add(StringTag.valueOf(s));
        tag.put("LearnedSkills", skills);

        tag.putInt("Earned", entry.earnedPoints);
        tag.putInt("Available", entry.availablePoints);
        // 开局角色书是否已发也要跟着走：这条 NBT 是换身/换壳搬家用的一条独立通路，
        // 漏掉的话换一次身就会"重置"，下次登录又白给一本
        tag.putBoolean("StarterBook", entry.starterBookGiven);
        return tag;
    }

    /** ★ 从 NBT 恢复某个玩家 UUID 的条目 */
    public void readNbt(UUID uuid, CompoundTag tag) {
        PlayerEntry entry = getEntry(uuid);
        entry.characterId = tag.getStringOr("CharacterId", MortalCharacter.ID);

        entry.learnedSkills.clear();
        tag.getList("LearnedSkills").ifPresent(list -> {
            for (Tag t : list) {
                // ⚠️ 26.2 的 StringTag#asString() 返回的是 Optional<String>，
                //    写成 String.valueOf(...) 会把技能存成 "Optional[thunder_power]"，
                //    换一次身体再套一层（Optional[Optional[...]]），技能树就全变"未解锁"了。
                //    这里必须取 value()（StringTag 是 record，value() 才是真正的字符串）。
                if (t instanceof StringTag s) entry.learnedSkills.add(normalizeSkillId(s.value()));
            }
        });

        entry.earnedPoints = tag.getIntOr("Earned", 0);
        entry.availablePoints = tag.getIntOr("Available", 0);
        entry.starterBookGiven = tag.getBooleanOr("StarterBook", false);

        setDirty();
    }

    public void clearLearnedSkills(UUID uuid) {
        getEntry(uuid).learnedSkills.clear();
        setDirty();
    }

    /**
     * 修掉老存档里已被写坏的技能 id。
     * <p>
     * 26.2 的 {@code StringTag#asString()} 返回 {@code Optional<String>}，早期代码写成
     * {@code String.valueOf(tag.asString())}，于是技能被存成了 {@code Optional[thunder_power]}，
     * 每换一次身体还会再多套一层壳 —— 技能树因此全部显示"未解锁"。
     * 这里把外面那几层 {@code Optional[...]} 剥掉，让已经中招的存档也能自动恢复。
     */
    private static String normalizeSkillId(String skill) {
        if (skill == null) {
            return null;
        }
        String value = skill;
        while (value.startsWith("Optional[") && value.endsWith("]")) {
            value = value.substring("Optional[".length(), value.length() - 1);
        }
        return value;
    }
}