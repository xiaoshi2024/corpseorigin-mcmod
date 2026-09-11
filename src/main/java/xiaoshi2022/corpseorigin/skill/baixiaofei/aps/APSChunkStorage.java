package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

public class APSChunkStorage {
    private final ConcurrentHashMap<Long, APSChunkData> chunks = new ConcurrentHashMap<>();
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private final Path storagePath;
    private final int storageMinY;
    private final int heightRange;
    private final List<CompletableFuture<Void>> pendingSaves = new ArrayList<>();

    public APSChunkStorage(Path storagePath, int storageMinY, int heightRange) {
        this.storagePath = storagePath;
        this.storageMinY = storageMinY;
        this.heightRange = heightRange;
    }

    private static long key(int x, int z) {
        return (long) (x >> 4) << 32 | (z >> 4) & 0xFFFFFFFFL;
    }

    private APSChunkData getOrCreate(int worldX, int worldZ) {
        long k = key(worldX, worldZ);
        return chunks.computeIfAbsent(k, kk -> {
            int cx = (int) (kk >> 32);
            int cz = (int) kk.longValue();
            return new APSChunkData(cx, cz, storageMinY, heightRange);
        });
    }

    public void saveBlock(BlockPos pos, BlockState state) {
        APSChunkData c = getOrCreate(pos.getX(), pos.getZ());
        c.saveBlock(pos.getX() & 15, pos.getZ() & 15, pos.getY(), state);
        dirtyChunks.add(key(pos.getX(), pos.getZ()));
    }

    public void saveBlockEntity(BlockPos pos, CompoundTag nbt) {
        APSChunkData c = getOrCreate(pos.getX(), pos.getZ());
        c.saveBlockEntity(pos.getX() & 15, pos.getZ() & 15, pos.getY(), nbt);
        dirtyChunks.add(key(pos.getX(), pos.getZ()));
    }

    public CompoundTag getBlockEntity(BlockPos pos) {
        APSChunkData c = chunks.get(key(pos.getX(), pos.getZ()));
        return c == null ? null : c.getBlockEntity(pos.getX() & 15, pos.getZ() & 15, pos.getY());
    }

    public BlockState getBlock(BlockPos pos) {
        APSChunkData c = chunks.get(key(pos.getX(), pos.getZ()));
        return c == null ? null : c.getBlock(pos.getX() & 15, pos.getZ() & 15, pos.getY());
    }

    public void saveBiome(BlockPos pos, ResourceKey<Biome> key) {
        APSChunkData c = getOrCreate(pos.getX(), pos.getZ());
        c.saveBiome(pos.getX() & 15, pos.getZ() & 15, pos.getY() - c.baseY, key);
        dirtyChunks.add(key(pos.getX(), pos.getZ()));
    }

    public ResourceKey<Biome> getBiomeKey(BlockPos pos) {
        APSChunkData c = chunks.get(key(pos.getX(), pos.getZ()));
        return c == null ? null : c.getBiome(pos.getX() & 15, pos.getZ() & 15, pos.getY() - c.baseY);
    }

    public Holder<Biome> getBiomeHolder(BlockPos pos, ServerLevel level) {
        ResourceKey<Biome> k = getBiomeKey(pos);
        return k == null ? null : level.registryAccess().lookupOrThrow(Registries.BIOME).get(k).orElse(null);
    }

    public Iterable<BlockPos> getBlockPositions() {
        List<BlockPos> list = new ArrayList<>();
        for (APSChunkData c : chunks.values()) {
            int wx = c.chunkX << 4, wz = c.chunkZ << 4;
            c.forEachBlock((lx, lz, y, s) -> list.add(new BlockPos(wx + lx, y, wz + lz)));
        }
        return list;
    }

    public Iterable<BlockPos> getBiomePositions() {
        List<BlockPos> list = new ArrayList<>();
        for (APSChunkData c : chunks.values()) {
            int wx = c.chunkX << 4, wz = c.chunkZ << 4;
            for (APSChunkData.BiomeEntry e : c.getBiomeEntries()) {
                list.add(new BlockPos(wx + (e.relX() & 255), c.baseY + e.relY(), wz + (e.relZ() & 255)));
            }
        }
        return list;
    }

    public void forEachXZPosition(LongConsumer c) {
        for (APSChunkData chunk : chunks.values()) {
            int wx = chunk.chunkX << 4, wz = chunk.chunkZ << 4;
            chunk.forEachBlockXZ((lx, lz) -> c.accept((long) (wx + lx) << 32 | (wz + lz) & 0xFFFFFFFFL));
        }
    }

    public int getBlockCount() {
        int c = 0;
        for (APSChunkData d : chunks.values()) c += d.getBlockCount();
        return c;
    }

    public CompletableFuture<Void> saveIncrementalAsync() {
        if (dirtyChunks.isEmpty()) return CompletableFuture.completedFuture(null);
        Map<Long, ChunkSnapshot> snaps = new HashMap<>();
        Iterator<Long> it = dirtyChunks.iterator();
        while (it.hasNext()) {
            long k = it.next();
            it.remove();
            APSChunkData c = chunks.get(k);
            if (c != null) {
                snaps.put(k, new ChunkSnapshot(c.chunkX, c.chunkZ, c.baseY, c.heightRange,
                        c.copyStateIds(), c.copyBiomeEntries(), c.copyBlockEntityNbt()));
            }
        }
        if (snaps.isEmpty()) return CompletableFuture.completedFuture(null);
        Path path = storagePath;
        CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(path);
                for (Map.Entry<Long, ChunkSnapshot> e : snaps.entrySet()) {
                    ChunkSnapshot s = e.getValue();
                    byte[] data = APSChunkData.serialize(s.chunkX, s.chunkZ, s.baseY, s.heightRange,
                            s.stateIds, s.biomeEntries, s.blockEntityNbt);
                    Files.write(path.resolve("chunk_" + s.chunkX + "_" + s.chunkZ + ".dat"), data);
                }
            } catch (IOException ex) {
                CorpseOrigin.LOGGER.error("APS save failed", ex);
            }
        }, Util.ioPool());
        synchronized (pendingSaves) {
            pendingSaves.removeIf(CompletableFuture::isDone);
            pendingSaves.add(f);
        }
        return f;
    }

    public void loadFromDisk() {
        if (!Files.isDirectory(storagePath)) return;
        try (Stream<Path> files = Files.list(storagePath)) {
            files.filter(p -> p.getFileName().toString().startsWith("chunk_")
                            && p.getFileName().toString().endsWith(".dat"))
                    .forEach(file -> {
                        try {
                            APSChunkData c = APSChunkData.deserialize(Files.readAllBytes(file));
                            chunks.put((long) c.chunkX << 32 | c.chunkZ & 0xFFFFFFFFL, c);
                        } catch (Exception e) {
                            CorpseOrigin.LOGGER.error("APS load failed {}", file, e);
                        }
                    });
        } catch (IOException e) {
            CorpseOrigin.LOGGER.error("APS list failed {}", storagePath, e);
        }
    }

    public CompletableFuture<Void> deleteAllAsync() {
        chunks.clear();
        dirtyChunks.clear();
        Path path = storagePath;
        return CompletableFuture.runAsync(() -> {
            try {
                if (Files.isDirectory(path)) {
                    try (Stream<Path> f = Files.list(path)) {
                        f.forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                    }
                    Files.deleteIfExists(path);
                }
            } catch (IOException ignored) {}
        }, Util.ioPool());
    }

    public void waitForPendingSaves() {
        List<CompletableFuture<Void>> toWait;
        synchronized (pendingSaves) {
            toWait = new ArrayList<>(pendingSaves);
            pendingSaves.clear();
        }
        for (CompletableFuture<Void> f : toWait) {
            try { f.join(); } catch (Exception ignored) {}
        }
    }

    public void clear() { chunks.clear(); dirtyChunks.clear(); }

    public Path getStoragePath() { return storagePath; }
    public int getStorageMinY() { return storageMinY; }
    public int getHeightRange() { return heightRange; }

    public static Path getPlayerStoragePath(ServerLevel level, UUID playerId) {
        Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);
        return worldDir.resolve("data").resolve("ancient_poetry_sword").resolve(playerId.toString());
    }

    private record ChunkSnapshot(int chunkX, int chunkZ, int baseY, int heightRange,
                                 int[] stateIds, List<APSChunkData.BiomeEntry> biomeEntries,
                                 Map<Integer, CompoundTag> blockEntityNbt) {}
}