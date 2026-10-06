package xiaoshi2022.corpseorigin.event;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skin.LocalSkinNames;

import java.util.List;

/**
 * 漫展尸兄的"误入尸潮"事件 —— <b>事件型周期召唤</b>。
 * <p>
 * 照搬 {@link GeckoZbrSpawns} 的模式：不进自然生成表，
 * 默认<b>第 3 天</b>首次现身、之后每 {@code intervalDays}（默认 15）天一次，
 * 在某个生存玩家附近 {@code minRadius~maxRadius} 格内一次放出一群
 * （{@code count} 只，默认 16）本地皮肤的漫展尸兄，并广播提示。
 * <p>
 * SavedData 存累计次数 → 每 200 tick 检查一次 →
 * ① 是否到了下一次该召唤的游戏日；② 本地皮肤名单非空（{@code config/corpseorigin/skins/<folder>}）；
 * ③ {@code nearbyCheck} 格内已有"漫展皮肤"尸兄就跳过（防堆叠）。
 * 和平难度 / 角色扮演模式不刷；{@code firstDay = 0} 关闭事件（指令不受影响）。
 * <p>
 * GUI 参数实时生效：每周期直接读 {@link CorpseConfig}，改完保存下个周期就按新参数算。
 */
public final class ManzhanSpawns {

    private ManzhanSpawns() {
    }

    /** 每只漫展尸兄寻找落脚点的最大尝试次数 */
    private static final int MAX_ATTEMPTS = 16;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 每 200 tick 检查一次（=10 秒），降低开销
            ServerLevel level = server.overworld();
            if (level.getGameTime() % 200 != 0
                    || level.getDifficulty() == Difficulty.PEACEFUL
                    || RoleplayMode.isEnabled(server)) return;

            var cfg = CorpseConfig.get().spawn.manzhan;
            if (cfg.firstDay <= 0 || !cfg.enabled) return;

            List<String> names = LocalSkinNames.listNames();
            if (names.isEmpty()) {
                // 名单为空说明制作者还没放皮肤：不再往下走（每10秒只空跑一个 listNames，开销可忽略）
                return;
            }

            final int minRadius = Math.max(8, cfg.minRadius);
            final int maxRadius = Math.max(minRadius, cfg.maxRadius);
            final int interval = Math.max(1, cfg.intervalDays);
            final int count = Math.max(1, cfg.count);
            final double nearbyCheck = Math.max(16, cfg.nearbyCheck);

            List<ServerPlayer> players = level.players().stream()
                    .filter(p -> !p.isSpectator() && !p.isCreative() && p.isAlive()).toList();
            if (players.isEmpty()) return;

            State state = level.getDataStorage().computeIfAbsent(State.TYPE);
            long day = xiaoshi2022.corpseorigin.util.WorldCalendar.dayTime(level) / 24000 + 1;
            if (day < nextDay(state.spawnCount, cfg.firstDay, interval)) return;

            ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
            int spawned = trySpawnGroup(level, target, names, minRadius, maxRadius, nearbyCheck, count);
            if (spawned > 0) {
                state.spawnCount++;
                state.setDirty();
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("message.corpseorigin.manzhan_arrival",
                                spawned, target.getDisplayName()), false);
                CorpseOrigin.LOGGER.info("漫展尸兄群（{} 只，本地皮肤 {} 个）在玩家 {} 附近现身（第 {} 次）",
                        spawned, names.size(), target.getName().getString(), state.spawnCount);
            }
        });
    }

    /** 第 n 次召唤对应的"该出现的游戏日" */
    private static long nextDay(int count, int firstDay, int intervalDays) {
        return firstDay + (long) count * intervalDays;
    }

    /** 附近已有漫展皮肤尸兄 → 跳过（防止堆叠；普通尸兄不算）。 */
    private static boolean hasNearbyManzhan(ServerLevel level, ServerPlayer player, double nearbyCheck) {
        return !level.getEntitiesOfClass(LowerLevelZbEntity.class,
                new AABB(player.blockPosition()).inflate(nearbyCheck),
                zb -> zb.isAlive() && LocalSkinNames.isLocal(zb.getPlayerSkinName())).isEmpty();
    }

    /** 放出一群漫展尸兄，返回实际生成数量。 */
    private static int trySpawnGroup(ServerLevel level, ServerPlayer player, List<String> names,
                                     int minRadius, int maxRadius, double nearbyCheck, int count) {
        if (hasNearbyManzhan(level, player, nearbyCheck)) return 0;

        var random = level.getRandom();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            LowerLevelZbEntity zb = spawnOne(level, player, minRadius, maxRadius, random);
            if (zb == null) continue;
            String skinName = names.get(random.nextInt(names.size()));
            zb.setPlayerSkinName(skinName);
            zb.setCustomId(skinName);
            spawned++;
        }
        return spawned;
    }

    /** 单只落地：环形散布 + 找稳固地面（不在水里），成功返回已入场的实体，失败 null。 */
    private static LowerLevelZbEntity spawnOne(ServerLevel level, ServerPlayer player,
                                               int minRadius, int maxRadius,
                                               net.minecraft.util.RandomSource random) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int dx = random.nextInt(maxRadius - minRadius + 1) + minRadius
                    * (random.nextBoolean() ? 1 : -1);
            int dz = random.nextInt(maxRadius - minRadius + 1) + minRadius
                    * (random.nextBoolean() ? 1 : -1);
            int x = player.getBlockX() + dx;
            int z = player.getBlockZ() + dz;
            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;

            LowerLevelZbEntity zb = ModEntities.LOWER_LEVEL_ZB.create(level, EntitySpawnReason.EVENT);
            if (zb == null) return null;
            zb.setPos(x + 0.5, pos.getY(), z + 0.5);
            if (!level.noCollision(zb) || !level.getFluidState(pos).isEmpty()) continue;
            if (level.addFreshEntity(zb)) return zb;
            return null;
        }
        return null;
    }

    private static final class State extends SavedData {
        private static final Codec<State> CODEC = Codec.INT.xmap(State::new, s -> s.spawnCount);
        private static final SavedDataType<State> TYPE = new SavedDataType<>(
                CorpseOrigin.id("manzhan_spawns"), State::new, CODEC, null);
        private int spawnCount;
        private State() {}
        private State(int spawnCount) { this.spawnCount = spawnCount; }
    }
}
