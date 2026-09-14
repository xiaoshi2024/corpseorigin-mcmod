package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.APSInkScenePayload;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

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

    /** 玩家 UUID → 领域内被移除的生物（NBT + 原位置） */
    private static final Map<UUID, List<RemovedMob>> playerRemovedMobs = new HashMap<>();

    public static final String APS_PICKED_TAG = "aps_blade_pickup";

    private static final int UPDATE_FLAGS = 306;
    private static final int SILENT_FLAGS = 2 | 16;

    /** ✅ 用蓝冰代替水（不流动，不会残留） */
    private static final BlockState FAKE_WATER = Blocks.BLUE_ICE.defaultBlockState();

    /** 被移除的生物：NBT + 原位置 */
    public record RemovedMob(CompoundTag nbt, BlockPos origin) {}

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

    // ==================== 生物处理 ====================

    /**
     * 展开时：把领域内所有生物（除玩家和河道上的）移除，保存 NBT + 原位置。
     */
    public static void removeMobsInRealm(Player player, ServerLevel level, BlockPos center,
                                         double riverDirX, double riverDirZ) {
        UUID pid = player.getUUID();
        List<RemovedMob> removed = new ArrayList<>();

        double searchRadius = APSTerrainGenerator.RIVER_HALF_WIDTH
                + APSTerrainGenerator.BANK_WIDTH
                + APSTerrainGenerator.MOUNTAIN_RUN + 30;
        AABB box = new AABB(
                center.getX() - searchRadius, level.getMinY(), center.getZ() - searchRadius,
                center.getX() + searchRadius, level.getMaxY(), center.getZ() + searchRadius);

        // ✅ 排除玩家和河道上的生物
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayer)
                        && !isOnRiver(e, center, riverDirX, riverDirZ));

        // ✅ 遍历 targets，保存 NBT，discard 生物
        for (LivingEntity t : targets) {
            CompoundTag nbt;
            net.minecraft.world.level.storage.TagValueOutput output =
                    net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                            net.minecraft.util.ProblemReporter.DISCARDING,
                            level.registryAccess());
            t.save(output);   // ✅ 用 save，保存完整数据（含 id、UUID、Brain）
            nbt = output.buildResult();

            removed.add(new RemovedMob(nbt, t.blockPosition().immutable()));
            t.discard();
        }

        playerRemovedMobs.put(pid, removed);

        CorpseOrigin.LOGGER.info("APS: removeMobsInRealm 移除 {} 个生物，playerRemovedMobs size={}",
                removed.size(), playerRemovedMobs.get(pid) == null ? "null" : playerRemovedMobs.get(pid).size());
    }

    /**
     * ✅ 判断生物是否在河道上（zone == 0）
     */
    private static boolean isOnRiver(LivingEntity entity, BlockPos center,
                                     double riverDirX, double riverDirZ) {
        int x = entity.blockPosition().getX();
        int z = entity.blockPosition().getZ();
        return APSTerrainGenerator.getZone(x, z, center, riverDirX, riverDirZ) == 0;
    }

    /**
     * 还原时：用 NBT 恢复所有被移除的生物。
     */
    public static void restoreRemovedMobs(Player player, ServerLevel level) {
        UUID pid = player.getUUID();

        CorpseOrigin.LOGGER.info("APS: restoreRemovedMobs 进入，playerRemovedMobs size={}",
                playerRemovedMobs.get(pid) == null ? "null" : playerRemovedMobs.get(pid).size());

        List<RemovedMob> removed = playerRemovedMobs.remove(pid);

        CorpseOrigin.LOGGER.info("APS: restoreRemovedMobs removed={}",
                removed == null ? "null" : removed.size());

        if (removed == null || removed.isEmpty()) return;

        int restored = 0;
        for (RemovedMob rm : removed) {
            String idStr = rm.nbt().getString("id").orElse("");
            Identifier id = Identifier.tryParse(idStr);
            if (id == null) continue;

            var ref = BuiltInRegistries.ENTITY_TYPE.get(id).orElse(null);
            if (ref == null) continue;
            EntityType<?> type = ref.value();

            Entity newEntity = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            if (newEntity == null) continue;

            newEntity.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    rm.nbt()));

            newEntity.setPos(
                    rm.origin().getX() + 0.5,
                    rm.origin().getY() + 1.0,
                    rm.origin().getZ() + 0.5);
            newEntity.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(newEntity);
            restored++;
        }

        CorpseOrigin.LOGGER.info("APS: 恢复 {} / {} 个生物", restored, removed.size());
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
        // ✅ 不再 put 空列表，避免覆盖已有数据

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
                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(pid);
                    if (sp != null) {
                        restoreRemovedMobs(sp, level);
                        // ✅ 广播"意境关"
                        broadcastInkSceneClose(level, sp, t.center);
                        sp.sendSystemMessage(Component.translatable(
                                "skill.corpseorigin.ancient_poetry_sword.restored"));
                    }
                    cleanupPlayerData(pid, level);
                } else {
                    t.snapshot.setTransformed(true);
                    String preset = playerPresetNames.get(pid);
                    ItemStack blade = playerBladeItems.getOrDefault(pid, ItemStack.EMPTY);
                    APSSavedData.get(level).saveSnapshot(pid, t.snapshot,
                            preset != null ? preset : "aps_default", blade);
                    t.snapshot.saveAsync();

                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(pid);
                    if (sp != null) {
                        broadcastInkSceneOpen(level, sp, t.center);
                        // ✅ 落第 1 句诗（朝辞白帝彩云间）
                        CorpseNetwork.broadcastInkPoem(sp, 0);
                        sp.sendSystemMessage(Component.translatable(
                                "skill.corpseorigin.ancient_poetry_sword.deployed"));
                    }
                }
            }
        }
    }

    private static void broadcastInkSceneOpen(ServerLevel level, ServerPlayer caster, BlockPos center) {
        double[] dir = playerRiverDir.getOrDefault(caster.getUUID(), new double[]{1.0, 0.0});
        APSInkScenePayload payload = new APSInkScenePayload(
                caster.getUUID(),
                center.getX(), center.getY(), center.getZ(),
                dir[0], dir[1],
                true);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void broadcastInkSceneClose(ServerLevel level, ServerPlayer caster, BlockPos center) {
        double[] dir = playerRiverDir.getOrDefault(caster.getUUID(), new double[]{1.0, 0.0});
        APSInkScenePayload payload = new APSInkScenePayload(
                caster.getUUID(),
                center.getX(), center.getY(), center.getZ(),
                dir[0], dir[1],
                false);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    // ==================== 清理 ====================

    private static void cleanupPlayerData(UUID pid, ServerLevel level) {
        CorpseOrigin.LOGGER.info("APS: cleanupPlayerData 进入，playerRemovedMobs size={}",
                playerRemovedMobs.get(pid) == null ? "null" : playerRemovedMobs.get(pid).size());

        TerrainSnapshot snap = playerSnapshots.remove(pid);
        playerPresetNames.remove(pid);
        playerBaseY.remove(pid);
        playerSeeds.remove(pid);
        playerBladeItems.remove(pid);
        playerRiverDir.remove(pid);
        playerRemovedMobs.remove(pid);
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
        // ✅ 不再 put 空列表，避免覆盖已有数据

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
        playerRemovedMobs.clear();
    }

    public static void clearPlayerData(UUID pid) {
        playerSnapshots.remove(pid);
        playerPresetNames.remove(pid);
        activeTasks.remove(pid);
        playerBaseY.remove(pid);
        playerSeeds.remove(pid);
        playerBladeItems.remove(pid);
        playerRiverDir.remove(pid);
        playerRemovedMobs.remove(pid);
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

        float expandProgress = 0f;
        boolean resentChunks = false;
        int lastParticleTick = 0;

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
                if (!resentChunks) {
                    resentChunks = true;
                    resendAffectedChunks();
                }
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

            expandProgress = (float) currentIndex / currentRadiusPositions.size();

            if (level.getGameTime() - lastParticleTick >= 2) {
                lastParticleTick = (int) level.getGameTime();
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                if (sp != null && sp.level() == level) {
                    int from = Math.max(0, currentIndex - 20);
                    for (int k = from; k < currentIndex; k++) {
                        BlockPos p = currentRadiusPositions.get(k);
                        level.sendParticles(ParticleTypes.END_ROD,
                                p.getX() + 0.5, baseY + 8, p.getZ() + 0.5,
                                1, 0, -0.4, 0, 0.1);
                    }
                }
            }

            return false;
        }

        void resendAffectedChunks() {
            Set<Long> chunkKeys = new HashSet<>();
            for (BlockPos p : currentRadiusPositions) {
                chunkKeys.add((long)(p.getX() >> 4) << 32 | (p.getZ() >> 4) & 0xFFFFFFFFL);
            }

            for (long k : chunkKeys) {
                int cx = (int)(k >> 32);
                int cz = (int) k;
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk != null) {
                    chunk.markUnsaved();
                }
            }

            CorpseOrigin.LOGGER.info("APS: 标记 {} 个区块为脏", chunkKeys.size());
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
                        clearContainerBeforeRemove(p);
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
                    clearContainerBeforeRemove(p);
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
                    if (!level.getBlockState(p).is(FAKE_WATER.getBlock())) {
                        level.setBlock(p, FAKE_WATER, SILENT_FLAGS);
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

        void clearContainerBeforeRemove(BlockPos pos) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
                container.setChanged();
            }
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
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                if (sp != null && sp.level() == level) {
                    for (int k = 0; k < Math.min(50, allRestorePositions.size()); k++) {
                        BlockPos p = allRestorePositions.get(level.getRandom().nextInt(allRestorePositions.size()));
                        level.sendParticles(ParticleTypes.CLOUD,
                                p.getX() + 0.5, baseY + 3, p.getZ() + 0.5,
                                1, 0.5, 1.0, 0.5, 0.1);
                    }
                }
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
                        level.setBlock(p, orig, SILENT_FLAGS);
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