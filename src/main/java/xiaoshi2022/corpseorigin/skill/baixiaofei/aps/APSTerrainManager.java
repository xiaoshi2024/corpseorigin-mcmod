package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class APSTerrainManager {

    private static final Map<UUID, TerrainSnapshot> playerSnapshots = new HashMap<>();
    private static final Map<UUID, String> playerPresetNames = new HashMap<>();
    private static final Map<UUID, TransformationTask> activeTasks = new HashMap<>();
    private static final Map<UUID, Integer> playerBaseY = new HashMap<>();
    private static final Map<UUID, Long> playerSeeds = new HashMap<>();
    private static final Map<UUID, ItemStack> playerBladeItems = new HashMap<>();
    public static final String APS_PICKED_TAG = "aps_blade_pickup";

    private static final int UPDATE_FLAGS = 306;

    // ==================== 对外入口 ====================

    public static void toggleTransformation(Player player, ItemStack stack, ServerLevel level) {
        UUID pid = player.getUUID();
        if (activeTasks.containsKey(pid)) return;

        TerrainSnapshot snap = playerSnapshots.get(pid);
        if (snap == null) {
            APSSavedData data = APSSavedData.get(level);
            if (data.hasSnapshot(pid)) {
                snap = loadSnapshotFromDisk(pid, level, data);
                if (snap != null) {
                    playerSnapshots.put(pid, snap);
                    playerPresetNames.put(pid, data.getPresetName(pid));
                }
            }
        }

        if (snap != null && snap.isTransformed()) {
            startRestore(player, level);
        } else {
            startTransformation(player, stack, level);
        }
    }

    public static boolean hasActiveAPS(Player player) {
        UUID pid = player.getUUID();
        return playerSnapshots.containsKey(pid) || activeTasks.containsKey(pid);
    }

    // ==================== 展开 ====================

    private static void startTransformation(Player player, ItemStack triggerItem, ServerLevel level) {
        UUID pid = player.getUUID();
        BlockPos center = player.blockPosition();
        long seed = level.getGameTime();
        Path path = APSChunkStorage.getPlayerStoragePath(level, pid);

        int storageMinY = center.getY() - 5;
        int heightRange = level.getMaxY() - storageMinY + 1;
        APSChunkStorage storage = new APSChunkStorage(path, storageMinY, heightRange);

        // 26.2 没有 level.getDayTime()，直接传 0L（原时间不记录，APS 不改时间）
        TerrainSnapshot snap = new TerrainSnapshot(center, 80, 0L, storage);

        playerSnapshots.put(pid, snap);
        playerPresetNames.put(pid, "aps_default");
        playerBaseY.put(pid, center.getY());
        playerSeeds.put(pid, seed);
        playerBladeItems.put(pid, triggerItem.copy());

        APSSavedData.get(level).saveSnapshotWithTransforming(pid, snap, "aps_default", seed, triggerItem);

        TransformationTask task = new TransformationTask(pid, level, center, snap, false, seed, triggerItem, player);
        activeTasks.put(pid, task);

        if (player instanceof ServerPlayer sp) {
            sp.getCooldowns().addCooldown(triggerItem, 999999);
        }
    }

    // ==================== 还原 ====================

    private static void startRestore(Player player, ServerLevel level) {
        UUID pid = player.getUUID();
        TerrainSnapshot snap = playerSnapshots.get(pid);
        if (snap == null) return;

        APSSavedData.get(level).setRestoring(pid, true);
        BlockPos restoreCenter = new BlockPos(
                player.blockPosition().getX(),
                snap.getCenter().getY(),
                player.blockPosition().getZ());
        ItemStack held = player.getMainHandItem();
        TransformationTask task = new TransformationTask(pid, level, restoreCenter, snap, true, 0L, held, player);
        activeTasks.put(pid, task);
    }

    // ==================== 服务器 tick ====================

    public static void tick(ServerLevel level) {
        if (activeTasks.isEmpty()) return;

        Iterator<Map.Entry<UUID, TransformationTask>> it = activeTasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TransformationTask> e = it.next();
            TransformationTask t = e.getValue();
            if (t.level != level) continue;

            if (t.tick()) {
                it.remove();
                UUID pid = e.getKey();
                if (t.isRestore) {
                    // 26.2 删掉改时间；APS 不改时间
                    cleanupPlayerData(pid, level);
                } else {
                    t.snapshot.setTransformed(true);
                    String preset = playerPresetNames.get(pid);
                    ItemStack blade = playerBladeItems.getOrDefault(pid, ItemStack.EMPTY);
                    APSSavedData.get(level).saveSnapshot(pid, t.snapshot,
                            preset != null ? preset : "aps_default", blade);
                    t.snapshot.saveAsync();
                }
            }
        }
    }

    // ==================== 清理 ====================

    private static void cleanupPlayerData(UUID pid, ServerLevel level) {
        TerrainSnapshot snap = playerSnapshots.remove(pid);
        playerPresetNames.remove(pid);
        playerBaseY.remove(pid);
        playerSeeds.remove(pid);
        playerBladeItems.remove(pid);
        APSSavedData.get(level).removeSnapshot(pid);
        if (snap != null) snap.getChunkStorage().deleteAllAsync();
    }

    // ==================== 存档加载 ====================

    private static TerrainSnapshot loadSnapshotFromDisk(UUID pid, ServerLevel level, APSSavedData data) {
        APSSavedData.SnapshotData d = data.getSnapshotData(pid);
        if (d == null) return null;

        Path path = APSChunkStorage.getPlayerStoragePath(level, pid);
        int storageMinY = d.center().getY() - 5;
        int heightRange = level.getMaxY() - storageMinY + 1;
        APSChunkStorage storage = new APSChunkStorage(path, storageMinY, heightRange);
        storage.loadFromDisk();

        TerrainSnapshot snap = new TerrainSnapshot(d.center(), d.radius(), d.originalDayTime(), storage);
        snap.setTransformed(d.transformed());
        for (BlockPos bp : storage.getBiomePositions()) {
            ResourceKey<Biome> key = storage.getBiomeKey(bp);
            if (key != null) snap.saveBiomeKey(bp, key, level);
        }
        return snap;
    }

    public static void onPlayerLogin(ServerPlayer player, ServerLevel level) {
        UUID pid = player.getUUID();
        APSSavedData data = APSSavedData.get(level);
        if (!data.hasSnapshot(pid)) return;

        TerrainSnapshot snap = loadSnapshotFromDisk(pid, level, data);
        if (snap == null) return;

        playerSnapshots.put(pid, snap);
        playerPresetNames.put(pid, data.getPresetName(pid));
        long seed = data.getSeed(pid);
        playerBaseY.put(pid, snap.getCenter().getY());
        playerSeeds.put(pid, seed);

        ItemStack blade = data.getBladeItem(pid);
        playerBladeItems.put(pid,
                blade != null && !blade.isEmpty() ? blade.copy() : new ItemStack(Items.IRON_SWORD));

        if (data.isRestoring(pid)) {
            BlockPos rc = new BlockPos(
                    player.blockPosition().getX(),
                    snap.getCenter().getY(),
                    player.blockPosition().getZ());
            activeTasks.put(pid, new TransformationTask(pid, level, rc, snap, true, 0L, blade, player));
        } else if (data.isTransforming(pid)) {
            activeTasks.put(pid, new TransformationTask(pid, level, snap.getCenter(), snap, false, seed, blade, player));
        }
    }

    // ==================== 保存 ====================

    public static void saveAllSnapshots(ServerLevel level) {
        for (TerrainSnapshot s : playerSnapshots.values()) s.saveAsync();
    }

    public static void waitForAllSaves() {
        for (TerrainSnapshot s : playerSnapshots.values()) s.getChunkStorage().waitForPendingSaves();
    }

    public static void clearAllCache() {
        playerSnapshots.clear();
        playerPresetNames.clear();
        activeTasks.clear();
        playerBaseY.clear();
        playerSeeds.clear();
        playerBladeItems.clear();
    }

    public static void clearPlayerData(UUID pid) {
        playerSnapshots.remove(pid);
        playerPresetNames.remove(pid);
        activeTasks.remove(pid);
        playerBaseY.remove(pid);
        playerSeeds.remove(pid);
        playerBladeItems.remove(pid);
    }

    // ==================== 任务 ====================

    private static class TransformationTask {
        final UUID playerId;
        final ServerLevel level;
        final BlockPos center;
        final TerrainSnapshot snapshot;
        final boolean isRestore;
        final long seed;
        final ItemStack bladeItem;
        final Player owner;

        int currentRadius = 0;
        final int maxRadius;

        static final int BASE_BUDGET = 12000;
        static final int SCALE = 400;
        static final int MAX_BUDGET = 30000;
        static final int MAX_CHUNKS = 20;
        static final int RESTORE_SURFACE_BUDGET = 6000;
        static final int RESTORE_UNDERGROUND_BUDGET = 3000;

        List<BlockPos> currentRadiusPositions = new ArrayList<>();
        int currentIndex = 0;
        boolean blockRestoreComplete = false;
        boolean biomeRestoreComplete = false;
        List<BlockPos> allRestorePositions = null;
        boolean surfaceSweepComplete = false;
        int surfaceRestoreIndex = 0;
        int undergroundRestoreIndex = 0;
        final Random bladeRandom;

        TransformationTask(UUID pid, ServerLevel level, BlockPos center,
                           TerrainSnapshot snap, boolean isRestore, long seed,
                           ItemStack blade, Player owner) {
            this.playerId = pid;
            this.level = level;
            this.center = center;
            this.snapshot = snap;
            this.isRestore = isRestore;
            this.maxRadius = isRestore ? snap.getRadius() : 80;
            this.seed = seed;
            this.bladeItem = blade;
            this.owner = owner;
            this.bladeRandom = new Random(seed + pid.hashCode());
        }

        int getBudget() {
            return Math.min(BASE_BUDGET + currentRadius * SCALE, MAX_BUDGET);
        }

        boolean tick() {
            return isRestore ? tickRestore() : tickTransform();
        }

        // ---------- 圆形展开（UBW 风格，APS 可改成裂缝） ----------

        List<BlockPos> circlePositions(int radius) {
            List<BlockPos> list = new ArrayList<>();
            if (radius == 0) {
                list.add(new BlockPos(center.getX(), 0, center.getZ()));
                return list;
            }
            int r2 = radius * radius;
            int ir2 = (radius - 1) * (radius - 1);
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 <= r2 && d2 > ir2)
                        list.add(new BlockPos(center.getX() + dx, 0, center.getZ() + dz));
                }
            return list;
        }

        boolean tickTransform() {
            int depth = 5;
            int baseY = center.getY();
            if (currentRadius > maxRadius) return true;

            if (currentRadiusPositions.isEmpty()) {
                currentRadiusPositions = circlePositions(currentRadius);
                currentIndex = 0;
            }

            int budget = getBudget();
            int used = 0;
            Set<Long> chunksThisTick = new HashSet<>();
            int i;
            for (i = currentIndex; i < currentRadiusPositions.size() && used < budget; i++) {
                BlockPos p = currentRadiusPositions.get(i);
                int x = p.getX(), z = p.getZ();
                long ck = (long) (x >> 4) << 32 | (z >> 4) & 0xFFFFFFFFL;
                if (!chunksThisTick.contains(ck) && chunksThisTick.size() >= MAX_CHUNKS) break;
                chunksThisTick.add(ck);
                saveOriginal(x, z, baseY, depth, baseY + 80);
                int surfaceY = APSTerrainGenerator.calculateHeight(x, z, center, maxRadius, seed);
                used += generateTerrain(x, z, surfaceY, baseY, depth, baseY + 80);
            }

            currentIndex = i;
            if (currentIndex >= currentRadiusPositions.size()) {
                currentRadius++;
                currentRadiusPositions.clear();
                currentIndex = 0;
                if (currentRadius % 10 == 0) snapshot.saveAsync();
            }
            return false;
        }

        void saveOriginal(int x, int z, int baseY, int depth, int columnTop) {
            for (int y = baseY - depth; y <= columnTop; y++) {
                if (y < level.getMinY() || y > level.getMaxY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = level.getBlockState(p);
                snapshot.saveBlock(p, s);
                snapshot.saveBiome(p, level.getBiome(p));
                if (s.hasBlockEntity()) {
                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null) {
                        CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());
                        snapshot.saveBlockEntity(p, nbt);
                    }
                }
            }
        }

        int generateTerrain(int x, int z, int surfaceY, int baseY, int depth, int columnTop) {
            int clearBottom = baseY - depth;
            int count = 0;

            for (int y = columnTop; y >= clearBottom; y--) {
                if (y < level.getMinY() || y > level.getMaxY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                    count++;
                }
            }

            for (int y = clearBottom; y <= surfaceY; y++) {
                if (y < level.getMinY() || y > level.getMaxY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                BlockState ns = y == clearBottom
                        ? Blocks.BEDROCK.defaultBlockState()
                        : y == surfaceY
                          ? Blocks.STONE.defaultBlockState()
                          : Blocks.DEEPSLATE.defaultBlockState();
                if (!level.getBlockState(p).equals(ns)) {
                    level.setBlock(p, ns, UPDATE_FLAGS);
                    count++;
                }
            }
            return count;
        }

        // ---------- 还原 ----------

        boolean tickRestore() {
            int baseY = center.getY();
            APSChunkStorage storage = snapshot.getChunkStorage();
            int saveBottom = storage.getStorageMinY();
            int saveTop = saveBottom + storage.getHeightRange() - 1;
            int surfaceSplit = baseY - 5;

            if (allRestorePositions == null) {
                allRestorePositions = collectRestorePositions();
                surfaceRestoreIndex = 0;
                undergroundRestoreIndex = 0;
            }

            if (blockRestoreComplete && !biomeRestoreComplete) {
                restoreBiomes();
                biomeRestoreComplete = true;
            }
            if (blockRestoreComplete && biomeRestoreComplete) return true;

            int used = 0;
            Set<Long> chunksThisTick = new HashSet<>();

            if (!surfaceSweepComplete) {
                int budget = RESTORE_SURFACE_BUDGET;
                int i;
                for (i = surfaceRestoreIndex; i < allRestorePositions.size() && used < budget; i++) {
                    BlockPos p = allRestorePositions.get(i);
                    long ck = (long) (p.getX() >> 4) << 32 | (p.getZ() >> 4) & 0xFFFFFFFFL;
                    if (!chunksThisTick.contains(ck) && chunksThisTick.size() >= MAX_CHUNKS) break;
                    chunksThisTick.add(ck);
                    used += restoreColumn(p.getX(), p.getZ(), surfaceSplit, saveTop);
                }
                surfaceRestoreIndex = i;
                if (surfaceRestoreIndex >= allRestorePositions.size()) surfaceSweepComplete = true;
            } else {
                int budget = RESTORE_UNDERGROUND_BUDGET;
                int i;
                for (i = undergroundRestoreIndex; i < allRestorePositions.size() && used < budget; i++) {
                    BlockPos p = allRestorePositions.get(i);
                    long ck = (long) (p.getX() >> 4) << 32 | (p.getZ() >> 4) & 0xFFFFFFFFL;
                    if (!chunksThisTick.contains(ck) && chunksThisTick.size() >= MAX_CHUNKS) break;
                    chunksThisTick.add(ck);
                    used += restoreColumn(p.getX(), p.getZ(), saveBottom, surfaceSplit - 1);
                }
                undergroundRestoreIndex = i;
                if (undergroundRestoreIndex >= allRestorePositions.size()) blockRestoreComplete = true;
            }
            return false;
        }

        List<BlockPos> collectRestorePositions() {
            Set<Long> xz = new HashSet<>();
            snapshot.getChunkStorage().forEachXZPosition(xz::add);
            Map<Long, List<BlockPos>> groups = new HashMap<>();
            for (long k : xz) {
                int x = (int) (k >> 32);
                int z = (int) k;
                long ck = (long) (x >> 4) << 32 | (z >> 4) & 0xFFFFFFFFL;
                groups.computeIfAbsent(ck, kk -> new ArrayList<>()).add(new BlockPos(x, 0, z));
            }
            int ccx = center.getX() >> 4;
            int ccz = center.getZ() >> 4;
            List<Long> keys = new ArrayList<>(groups.keySet());
            keys.sort((a, b) -> {
                int ax = (int) (a >> 32) - ccx;
                int az = (int) (a & 0xFFFFFFFFL) - ccz;
                int bx = (int) (b >> 32) - ccx;
                int bz = (int) (b & 0xFFFFFFFFL) - ccz;
                return Integer.compare(ax * ax + az * az, bx * bx + bz * bz);
            });
            List<BlockPos> list = new ArrayList<>();
            for (long k : keys) list.addAll(groups.get(k));
            return list;
        }

        int restoreColumn(int x, int z, int minY, int maxY) {
            int count = 0;
            for (int y = minY; y <= maxY; y++) {
                if (y < level.getMinY() || y > level.getMaxY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                BlockState orig = snapshot.getBlock(p);
                if (orig != null) {
                    if (!level.getBlockState(p).equals(orig)) {
                        level.setBlock(p, orig, UPDATE_FLAGS);
                        count++;
                    }
                    CompoundTag nbt = snapshot.getBlockEntity(p);
                    if (nbt != null && orig.hasBlockEntity()) {
                        BlockEntity be = level.getBlockEntity(p);
                        if (be != null) {
                            ScopedCollector rep = new ScopedCollector(be.problemPath(), CorpseOrigin.LOGGER);
                            try {
                                be.loadWithComponents(TagValueInput.create(rep, level.registryAccess(), nbt));
                            } catch (Throwable ignored) {
                            } finally {
                                try { rep.close(); } catch (Throwable ignored) {}
                            }
                            be.setChanged();
                        }
                    }
                }
            }
            return count;
        }

        void restoreBiomes() {
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos p : snapshot.getBiomePositions()) {
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minZ = Math.min(minZ, p.getZ());
                maxZ = Math.max(maxZ, p.getZ());
            }
            if (minX == Integer.MAX_VALUE) return;

            List<ChunkAccess> chunks = new ArrayList<>();
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++)
                for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                    ChunkAccess c = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (c != null) chunks.add(c);
                }

            for (ChunkAccess c : chunks) {
                c.fillBiomesFromNoise(makeRestoreResolver(c), level.getChunkSource().randomState().sampler());
                c.markUnsaved();
            }
            level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
        }

        BiomeResolver makeRestoreResolver(ChunkAccess chunk) {
            return (x, y, z, sampler) -> {
                int bx = QuartPos.toBlock(x);
                int by = QuartPos.toBlock(y);
                int bz = QuartPos.toBlock(z);
                BlockPos p = new BlockPos(bx, by, bz);
                Holder<Biome> saved = snapshot.getBiome(p, level);
                return saved != null ? saved : chunk.getNoiseBiome(x, y, z);
            };
        }
    }
}