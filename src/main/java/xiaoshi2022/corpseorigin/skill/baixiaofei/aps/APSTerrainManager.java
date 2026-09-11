package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.nio.file.Path;
import java.util.*;

import static xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainGenerator.COLUMN_TOP_OFFSET;
import static xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainGenerator.PLATFORM_DEPTH;

public class APSTerrainManager {

    private static final Map<UUID, TerrainSnapshot> playerSnapshots = new HashMap<>();
    private static final Map<UUID, String> playerPresetNames = new HashMap<>();
    private static final Map<UUID, TransformationTask> activeTasks = new HashMap<>();
    private static final Map<UUID, Integer> playerBaseY = new HashMap<>();
    private static final Map<UUID, Long> playerSeeds = new HashMap<>();
    private static final Map<UUID, ItemStack> playerBladeItems = new HashMap<>();
    private static final Map<UUID, double[]> playerRiverDir = new HashMap<>();

    /** 玩家 UUID → (生物 UUID → 搬移前的原位置) */
    private static final Map<UUID, Map<UUID, BlockPos>> playerMovedMobs = new HashMap<>();

    public static final String APS_PICKED_TAG = "aps_blade_pickup";

    private static final int UPDATE_FLAGS = 306;
    private static final int SILENT_FLAGS = 2 | 16;

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

    public static double[] getRiverDir(Player player) {
        return playerRiverDir.getOrDefault(player.getUUID(), new double[]{1.0, 0.0});
    }

    /** 记录生物被搬移前的原位置（只记第一次） */
    public static void recordMobOrigin(Player player, LivingEntity mob) {
        UUID pid = player.getUUID();
        playerMovedMobs
                .computeIfAbsent(pid, k -> new HashMap<>())
                .putIfAbsent(mob.getUUID(), mob.blockPosition().immutable());
    }

    /** 还原：把之前搬移的生物 teleport 回原位置 */
    public static void restoreMovedMobs(Player player, ServerLevel level) {
        UUID pid = player.getUUID();
        Map<UUID, BlockPos> map = playerMovedMobs.remove(pid);
        if (map == null || map.isEmpty()) return;

        for (Map.Entry<UUID, BlockPos> e : map.entrySet()) {
            Entity entity = level.getEntity(e.getKey());
            if (entity instanceof LivingEntity living && living.isAlive()) {
                BlockPos origin = e.getValue();
                living.teleportTo(
                        origin.getX() + 0.5,
                        origin.getY() + 1.0,
                        origin.getZ() + 0.5);
                living.setDeltaMovement(Vec3.ZERO);
                living.hurtMarked = true;
            }
        }
    }

    // ==================== 展开 ====================

    private static void startTransformation(Player player, ItemStack triggerItem, ServerLevel level) {
        UUID pid = player.getUUID();
        BlockPos center = player.blockPosition();
        long seed = level.getGameTime();
        Path path = APSChunkStorage.getPlayerStoragePath(level, pid);

        int storageMinY = center.getY() - 5 - PLATFORM_DEPTH;
        int heightRange = level.getMaxY() - storageMinY + 1;
        APSChunkStorage storage = new APSChunkStorage(path, storageMinY, heightRange);

        TerrainSnapshot snap = new TerrainSnapshot(center, 80, 0L, storage);

        playerSnapshots.put(pid, snap);
        playerPresetNames.put(pid, "aps_default");
        playerBaseY.put(pid, center.getY());
        playerSeeds.put(pid, seed);
        playerBladeItems.put(pid, triggerItem.copy());
        playerMovedMobs.put(pid, new HashMap<>());   // ✅ 初始化生物位置记录

        Vec3 look = player.getLookAngle();
        double len = Math.sqrt(look.x * look.x + look.z * look.z);
        double[] dir;
        if (len < 0.001) {
            dir = new double[]{1.0, 0.0};
        } else {
            dir = new double[]{look.x / len, look.z / len};
        }
        playerRiverDir.put(pid, dir);

        APSSavedData.get(level).saveSnapshotWithTransforming(pid, snap, "aps_default", seed, triggerItem);

        TransformationTask task = new TransformationTask(pid, level, center, snap, false, seed, triggerItem, player);
        activeTasks.put(pid, task);
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
                    // ✅ 先还原生物位置
                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(pid);
                    if (sp != null) {
                        restoreMovedMobs(sp, level);
                    }
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
        playerRiverDir.remove(pid);
        playerMovedMobs.remove(pid);
        APSSavedData.get(level).removeSnapshot(pid);
        if (snap != null) snap.getChunkStorage().deleteAllAsync();
    }

    // ==================== 存档加载 ====================

    private static TerrainSnapshot loadSnapshotFromDisk(UUID pid, ServerLevel level, APSSavedData data) {
        APSSavedData.SnapshotData d = data.getSnapshotData(pid);
        if (d == null) return null;

        Path path = APSChunkStorage.getPlayerStoragePath(level, pid);
        int storageMinY = d.center().getY() - 5 - PLATFORM_DEPTH;
        int heightRange = level.getMaxY() - storageMinY + 1;
        APSChunkStorage storage = new APSChunkStorage(path, storageMinY, heightRange);
        storage.loadFromDisk();

        TerrainSnapshot snap = new TerrainSnapshot(d.center(), d.radius(), d.originalDayTime(), storage);
        snap.setTransformed(d.transformed());
        for (BlockPos bp : storage.getBiomePositions()) {
            ResourceKey<Biome> key = storage.getBiomeKey(bp);
            if (key != null) snap.saveBiomeKey(bp, key, level);
        }
        playerRiverDir.putIfAbsent(pid, new double[]{1.0, 0.0});
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
        playerMovedMobs.put(pid, new HashMap<>());

        BlockPos rc = new BlockPos(
                player.blockPosition().getX(),
                snap.getCenter().getY(),
                player.blockPosition().getZ());
        activeTasks.put(pid, new TransformationTask(pid, level, rc, snap, true, 0L, blade, player));

        CorpseOrigin.LOGGER.info("APS: 玩家 {} 重进，自动回收剑意", player.getName().getString());
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
        playerRiverDir.clear();
        playerMovedMobs.clear();
    }

    public static void clearPlayerData(UUID pid) {
        playerSnapshots.remove(pid);
        playerPresetNames.remove(pid);
        activeTasks.remove(pid);
        playerBaseY.remove(pid);
        playerSeeds.remove(pid);
        playerBladeItems.remove(pid);
        playerRiverDir.remove(pid);
        playerMovedMobs.remove(pid);
    }

    public static boolean isBusy(Player player) {
        return activeTasks.containsKey(player.getUUID());
    }

    public static void forceRestoreOnDisconnect(Player player, ServerLevel level) {
        UUID pid = player.getUUID();
        activeTasks.remove(pid);

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

        if (snap == null || !snap.isTransformed()) {
            cleanupPlayerData(pid, level);
            return;
        }

        startRestore(player, level);
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
        final double riverDirX;
        final double riverDirZ;

        int currentRadius = 0;
        final int maxRadius;

        static final int MAX_BUDGET = 8000;
        static final int MAX_CHUNKS = 8;
        static final int RESTORE_SURFACE_BUDGET = 12000;
        static final int RESTORE_UNDERGROUND_BUDGET = 6000;

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
            this.maxRadius = isRestore ? snap.getRadius() : APSTerrainGenerator.LENGTH / 2;
            this.seed = seed;
            this.bladeItem = blade;
            this.owner = owner;
            this.bladeRandom = new Random(seed + pid.hashCode());

            double[] dir = playerRiverDir.getOrDefault(pid, new double[]{1.0, 0.0});
            this.riverDirX = dir[0];
            this.riverDirZ = dir[1];
        }

        int getBudget() {
            return MAX_BUDGET;
        }

        boolean tick() {
            return isRestore ? tickRestore() : tickTransform();
        }

        // ---------- 一剑山河展开 ----------

        List<BlockPos> swordLandPositions() {
            List<BlockPos> list = new ArrayList<>();
            int perpMax = APSTerrainGenerator.RIVER_HALF_WIDTH
                    + APSTerrainGenerator.BANK_WIDTH
                    + APSTerrainGenerator.MOUNTAIN_RUN;
            int half = perpMax + 1;
            int alongHalf = APSTerrainGenerator.LENGTH / 2 + 1;

            double perpDirX = -riverDirZ;
            double perpDirZ = riverDirX;

            Set<Long> seen = new HashSet<>();
            for (int s = -alongHalf; s <= alongHalf; s++) {
                double baseX = center.getX() + riverDirX * s;
                double baseZ = center.getZ() + riverDirZ * s;
                for (int p = -half; p <= half; p++) {
                    int x = (int) Math.round(baseX + perpDirX * p);
                    int z = (int) Math.round(baseZ + perpDirZ * p);
                    long k = (long) x << 32 | z & 0xFFFFFFFFL;
                    if (!seen.add(k)) continue;
                    if (APSTerrainGenerator.getZone(x, z, center, riverDirX, riverDirZ) < 0) continue;
                    list.add(new BlockPos(x, 0, z));
                }
            }
            return list;
        }

        boolean tickTransform() {
            int depth = 5;
            int baseY = center.getY();

            if (currentRadiusPositions.isEmpty() && currentIndex == 0) {
                currentRadiusPositions = swordLandPositions();
                CorpseOrigin.LOGGER.info("APS: 山河总列数 {}", currentRadiusPositions.size());
            }

            if (currentIndex >= currentRadiusPositions.size()) {
                return true;
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

                int zone = APSTerrainGenerator.getZone(x, z, center, riverDirX, riverDirZ);
                if (zone < 0) continue;

                int clearBottom = baseY - depth - PLATFORM_DEPTH;
                int columnTop = baseY + COLUMN_TOP_OFFSET;
                saveOriginal(x, z, clearBottom, columnTop);
                used += generateTerrain(x, z, baseY, depth, columnTop, zone);
            }

            currentIndex = i;
            if (currentIndex % 2000 == 0) snapshot.saveAsync();
            return false;
        }

        void saveOriginal(int x, int z, int bottom, int top) {
            for (int y = bottom; y <= top; y++) {
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

        int generateTerrain(int x, int z, int baseY, int depth, int columnTop, int zone) {
            int count = 0;

            int dxp = x - center.getX();
            int dzp = z - center.getZ();
            boolean nearPlayer = (dxp * dxp + dzp * dzp <= 9);

            if (nearPlayer) {
                for (int y = columnTop; y > baseY; y--) {
                    if (y < level.getMinY() || y > level.getMaxY()) continue;
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), SILENT_FLAGS);
                        count++;
                    }
                }
                return count;
            }

            int clearBottom = baseY - depth - PLATFORM_DEPTH;
            int landY = APSTerrainGenerator.calculateSwordLandHeight(
                    x, z, center, seed, riverDirX, riverDirZ);

            for (int y = columnTop; y >= clearBottom; y--) {
                if (y < level.getMinY() || y > level.getMaxY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), SILENT_FLAGS);
                    count++;
                }
            }

            for (int y = clearBottom; y <= landY; y++) {
                if (y < level.getMinY() || y > level.getMaxY()) continue;
                BlockPos p = new BlockPos(x, y, z);
                BlockState ns;
                if (y == clearBottom) {
                    ns = Blocks.BEDROCK.defaultBlockState();
                } else if (y == landY) {
                    if (zone == 0) {
                        ns = Blocks.GRAVEL.defaultBlockState();
                    } else {
                        ns = Blocks.GRASS_BLOCK.defaultBlockState();
                    }
                } else if (y >= landY - 3) {
                    if (zone == 0) {
                        ns = Blocks.SAND.defaultBlockState();
                    } else {
                        ns = Blocks.DIRT.defaultBlockState();
                    }
                } else {
                    ns = Blocks.DEEPSLATE.defaultBlockState();
                }
                if (!level.getBlockState(p).equals(ns)) {
                    level.setBlock(p, ns, SILENT_FLAGS);
                    count++;
                }
            }

            if (zone == 0) {
                for (int y = landY + 1; y <= baseY - 1; y++) {
                    if (y < level.getMinY() || y > level.getMaxY()) continue;
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).is(Blocks.WATER)) {
                        level.setBlock(p, Blocks.WATER.defaultBlockState(), SILENT_FLAGS);
                        count++;
                    }
                }
            }

            if (zone == 2) {
                int h = landY - baseY;
                long r = (x * 73856093L) ^ (z * 19349663L) ^ seed;

                if ((r & 0xFF) < 20) {
                    BlockPos above = new BlockPos(x, landY + 1, z);
                    if (above.getY() <= level.getMaxY()
                            && level.getBlockState(above).isAir()) {
                        level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), SILENT_FLAGS);
                        count++;
                    }
                }

                if (h < 15 && ((r >> 8) & 0xFF) < 3) {
                    BlockPos above = new BlockPos(x, landY + 1, z);
                    if (above.getY() <= level.getMaxY()
                            && level.getBlockState(above).isAir()) {
                        BlockState flower = (((r >> 16) & 1) == 0)
                                ? Blocks.DANDELION.defaultBlockState()
                                : Blocks.POPPY.defaultBlockState();
                        level.setBlock(above, flower, SILENT_FLAGS);
                        count++;
                    }
                }

                if (h < 10 && ((r >> 24) & 0xFF) < 1) {
                    tryPlaceOakTree(level, x, landY + 1, z);
                }
            }

            return count;
        }

        void tryPlaceOakTree(ServerLevel level, int x, int y, int z) {
            if (y + 5 > level.getMaxY()) return;

            for (int dy = 0; dy <= 5; dy++) {
                BlockPos p = new BlockPos(x, y + dy, z);
                BlockState s = level.getBlockState(p);
                if (!s.isAir()
                        && !s.is(Blocks.SHORT_GRASS)
                        && !s.is(Blocks.DANDELION)
                        && !s.is(Blocks.POPPY)) {
                    return;
                }
            }

            for (int dy = 0; dy < 3; dy++) {
                level.setBlock(new BlockPos(x, y + dy, z),
                        Blocks.OAK_LOG.defaultBlockState(), SILENT_FLAGS);
            }

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 2; dy <= 4; dy++) {
                        int d2 = dx * dx + dz * dz + (dy - 3) * (dy - 3);
                        if (d2 > 6) continue;
                        if (dx == 0 && dz == 0 && dy < 3) continue;
                        BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                        if (p.getY() > level.getMaxY()) continue;
                        if (level.getBlockState(p).isAir()) {
                            level.setBlock(p, Blocks.OAK_LEAVES.defaultBlockState(), SILENT_FLAGS);
                        }
                    }
                }
            }
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
            if (blockRestoreComplete && biomeRestoreComplete) {
                clearResidualWater();   // ✅ 加这一步
                return true;
            }
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

        void clearResidualWater() {
            int baseY = center.getY();
            for (BlockPos xz : allRestorePositions) {
                int zone = APSTerrainGenerator.getZone(
                        xz.getX(), xz.getZ(), center, riverDirX, riverDirZ);
                if (zone != 0 && zone != 1) continue;

                for (int y = baseY - 10; y <= baseY + 2; y++) {
                    if (y < level.getMinY() || y > level.getMaxY()) continue;
                    BlockPos p = new BlockPos(xz.getX(), y, xz.getZ());
                    BlockState cur = level.getBlockState(p);
                    if (cur.is(Blocks.WATER)) {
                        BlockState orig = snapshot.getBlock(p);
                        if (orig != null && !orig.is(Blocks.WATER)) {
                            level.setBlock(p, orig, UPDATE_FLAGS);
                        } else if (orig == null) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                        }
                    }
                }
            }
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
                    BlockState cur = level.getBlockState(p);
                    if (!cur.equals(orig)) {
                        // ✅ 涉及水的还原用 UPDATE_FLAGS，触发流体更新
                        int flags = (cur.is(Blocks.WATER) || orig.is(Blocks.WATER))
                                ? UPDATE_FLAGS : SILENT_FLAGS;
                        level.setBlock(p, orig, flags);
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

    public static boolean forceRestore(Player player, ServerLevel level) {
        UUID pid = player.getUUID();

        TransformationTask existing = activeTasks.get(pid);
        if (existing != null && existing.isRestore) {
            return false;
        }
        if (existing != null) {
            activeTasks.remove(pid);
            CorpseOrigin.LOGGER.info("APS: 取消展开，强制还原");
        }

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

        if (snap == null) {
            return false;
        }

        startRestore(player, level);
        return true;
    }
}