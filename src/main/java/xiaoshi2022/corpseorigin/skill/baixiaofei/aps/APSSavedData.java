package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.*;

public class APSSavedData extends SavedData {

    private static final Codec<SnapshotData> SNAPSHOT_CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("center").forGetter(SnapshotData::center),
            Codec.INT.fieldOf("radius").forGetter(SnapshotData::radius),
            Codec.LONG.fieldOf("original_day_time").forGetter(SnapshotData::originalDayTime),
            Codec.BOOL.fieldOf("transformed").forGetter(SnapshotData::transformed),
            Codec.BOOL.optionalFieldOf("restoring", false).forGetter(SnapshotData::restoring),
            Codec.BOOL.optionalFieldOf("transforming", false).forGetter(SnapshotData::transforming),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(SnapshotData::seed),
            Codec.STRING.fieldOf("preset_name").forGetter(SnapshotData::presetName),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("blade_item", ItemStack.EMPTY).forGetter(SnapshotData::bladeItem)
    ).apply(i, SnapshotData::new));

    private static final Codec<PlayerEntry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(PlayerEntry::playerId),
            SNAPSHOT_CODEC.fieldOf("snapshot").forGetter(PlayerEntry::snapshot)
    ).apply(i, PlayerEntry::new));

    public static final Codec<APSSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("players", List.of())
                    .forGetter(d -> d.playerSnapshots.entrySet().stream()
                            .map(e -> new PlayerEntry(e.getKey(), e.getValue())).toList())
    ).apply(i, APSSavedData::new));

    public static final SavedDataType<APSSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "aps_data"),
            APSSavedData::new, CODEC, null);

    private final Map<UUID, SnapshotData> playerSnapshots = new HashMap<>();

    public APSSavedData() {}

    private APSSavedData(List<PlayerEntry> entries) {
        for (PlayerEntry e : entries) playerSnapshots.put(e.playerId(), e.snapshot());
    }

    public static APSSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void saveSnapshot(UUID pid, TerrainSnapshot snap, String preset, ItemStack blade) {
        playerSnapshots.put(pid, new SnapshotData(snap.getCenter(), snap.getRadius(),
                snap.getOriginalDayTime(), snap.isTransformed(), false, false, 0L,
                preset, blade != null ? blade.copy() : ItemStack.EMPTY));
        setDirty();
    }

    public void saveSnapshotWithTransforming(UUID pid, TerrainSnapshot snap, String preset,
                                             long seed, ItemStack blade) {
        playerSnapshots.put(pid, new SnapshotData(snap.getCenter(), snap.getRadius(),
                snap.getOriginalDayTime(), false, false, true, seed,
                preset, blade != null ? blade.copy() : ItemStack.EMPTY));
        setDirty();
    }

    public SnapshotData getSnapshotData(UUID pid) { return playerSnapshots.get(pid); }
    public String getPresetName(UUID pid) {
        SnapshotData d = playerSnapshots.get(pid);
        return d != null ? d.presetName() : null;
    }
    public ItemStack getBladeItem(UUID pid) {
        SnapshotData d = playerSnapshots.get(pid);
        return d != null ? d.bladeItem() : ItemStack.EMPTY;
    }
    public boolean hasSnapshot(UUID pid) { return playerSnapshots.containsKey(pid); }
    public boolean isTransformed(UUID pid) {
        SnapshotData d = playerSnapshots.get(pid);
        return d != null && d.transformed();
    }
    public boolean isRestoring(UUID pid) {
        SnapshotData d = playerSnapshots.get(pid);
        return d != null && d.restoring();
    }
    public boolean isTransforming(UUID pid) {
        SnapshotData d = playerSnapshots.get(pid);
        return d != null && d.transforming();
    }
    public long getSeed(UUID pid) {
        SnapshotData d = playerSnapshots.get(pid);
        return d != null ? d.seed() : 0L;
    }
    public void removeSnapshot(UUID pid) {
        if (playerSnapshots.remove(pid) != null) setDirty();
    }
    public void setTransformed(UUID pid, boolean b) {
        SnapshotData d = playerSnapshots.get(pid);
        if (d != null) { playerSnapshots.put(pid, d.withTransformed(b)); setDirty(); }
    }
    public void setRestoring(UUID pid, boolean b) {
        SnapshotData d = playerSnapshots.get(pid);
        if (d != null) { playerSnapshots.put(pid, d.withRestoring(b)); setDirty(); }
    }
    public void setTransforming(UUID pid, boolean b) {
        SnapshotData d = playerSnapshots.get(pid);
        if (d != null) { playerSnapshots.put(pid, d.withTransforming(b)); setDirty(); }
    }
    public Set<UUID> getPlayerIds() { return new HashSet<>(playerSnapshots.keySet()); }

    public record PlayerEntry(UUID playerId, SnapshotData snapshot) {}

    public record SnapshotData(
            BlockPos center, int radius, long originalDayTime,
            boolean transformed, boolean restoring, boolean transforming,
            long seed, String presetName, ItemStack bladeItem
    ) {
        public SnapshotData withTransformed(boolean b) {
            return new SnapshotData(center, radius, originalDayTime, b, restoring, transforming, seed, presetName, bladeItem);
        }
        public SnapshotData withRestoring(boolean b) {
            return new SnapshotData(center, radius, originalDayTime, transformed, b, transforming, seed, presetName, bladeItem);
        }
        public SnapshotData withTransforming(boolean b) {
            return new SnapshotData(center, radius, originalDayTime, transformed, restoring, b, seed, presetName, bladeItem);
        }
    }
}