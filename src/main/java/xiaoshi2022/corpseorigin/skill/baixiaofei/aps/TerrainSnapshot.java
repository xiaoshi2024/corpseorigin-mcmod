package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TerrainSnapshot {
    private final BlockPos center;
    private final int radius;
    private final long originalDayTime;
    private boolean transformed;
    private final APSChunkStorage chunkStorage;
    private final Map<BlockPos, Holder<Biome>> biomeHolderCache = new HashMap<>();
    private List<BlockPos> sortedPositions;

    public TerrainSnapshot(BlockPos center, int radius, long originalDayTime, APSChunkStorage storage) {
        this.center = center;
        this.radius = radius;
        this.originalDayTime = originalDayTime;
        this.chunkStorage = storage;
    }

    public void saveBlock(BlockPos pos, BlockState state) {
        chunkStorage.saveBlock(pos.immutable(), state);
        sortedPositions = null;
    }

    public void saveBiome(BlockPos pos, Holder<Biome> biome) {
        BlockPos bp = new BlockPos(pos.getX() >> 2 << 2, pos.getY() >> 2 << 2, pos.getZ() >> 2 << 2);
        if (!biomeHolderCache.containsKey(bp)) {
            biomeHolderCache.put(bp, biome);
            biome.unwrapKey().ifPresent(k -> chunkStorage.saveBiome(bp, k));
        }
    }

    public void saveBiomeKey(BlockPos pos, ResourceKey<Biome> key, ServerLevel level) {
        BlockPos bp = new BlockPos(pos.getX() >> 2 << 2, pos.getY() >> 2 << 2, pos.getZ() >> 2 << 2);
        chunkStorage.saveBiome(bp, key);
        level.registryAccess().lookupOrThrow(Registries.BIOME).get(key)
                .ifPresent(h -> biomeHolderCache.put(bp, h));
    }

    public void saveBlockEntity(BlockPos pos, CompoundTag nbt) {
        chunkStorage.saveBlockEntity(pos.immutable(), nbt);
    }

    public CompoundTag getBlockEntity(BlockPos pos) { return chunkStorage.getBlockEntity(pos); }
    public BlockState getBlock(BlockPos pos) { return chunkStorage.getBlock(pos); }

    public Holder<Biome> getBiome(BlockPos pos) {
        BlockPos bp = new BlockPos(pos.getX() >> 2 << 2, pos.getY() >> 2 << 2, pos.getZ() >> 2 << 2);
        return biomeHolderCache.get(bp);
    }

    public Holder<Biome> getBiome(BlockPos pos, ServerLevel level) {
        Holder<Biome> c = getBiome(pos);
        if (c != null) return c;
        BlockPos bp = new BlockPos(pos.getX() >> 2 << 2, pos.getY() >> 2 << 2, pos.getZ() >> 2 << 2);
        return chunkStorage.getBiomeHolder(bp, level);
    }

    public Iterable<BlockPos> getBiomePositions() { return chunkStorage.getBiomePositions(); }

    public List<BlockPos> getBlockPositionsSortedByDistance(Random random) {
        if (sortedPositions == null) {
            List<BlockPos> all = new ArrayList<>();
            for (BlockPos p : chunkStorage.getBlockPositions()) all.add(p);
            Map<Integer, List<BlockPos>> groups = new HashMap<>();
            for (BlockPos p : all) {
                int dx = p.getX() - center.getX();
                int dz = p.getZ() - center.getZ();
                int bucket = (int) Math.sqrt(dx * dx + dz * dz) / 5;
                groups.computeIfAbsent(bucket, k -> new ArrayList<>()).add(p);
            }
            sortedPositions = new ArrayList<>();
            List<Integer> keys = new ArrayList<>(groups.keySet());
            Collections.sort(keys);
            for (int k : keys) {
                List<BlockPos> g = groups.get(k);
                Collections.shuffle(g, random);
                sortedPositions.addAll(g);
            }
        }
        return sortedPositions;
    }

    public BlockPos getCenter() { return center; }
    public int getRadius() { return radius; }
    public long getOriginalDayTime() { return originalDayTime; }
    public boolean isTransformed() { return transformed; }
    public void setTransformed(boolean b) { transformed = b; }
    public int getBlockCount() { return chunkStorage.getBlockCount(); }
    public APSChunkStorage getChunkStorage() { return chunkStorage; }
    public CompletableFuture<Void> saveAsync() { return chunkStorage.saveIncrementalAsync(); }

    public void clear() {
        chunkStorage.clear();
        biomeHolderCache.clear();
        sortedPositions = null;
    }
}