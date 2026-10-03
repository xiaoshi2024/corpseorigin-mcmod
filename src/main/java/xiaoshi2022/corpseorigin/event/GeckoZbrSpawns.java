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
import xiaoshi2022.corpseorigin.entity.GeckoZbrEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.List;

/**
 * 壁虎奇葩尸兄的"生存遇见"机制 —— <b>事件型周期召唤</b>。
 * <p>
 * 完全照搬 {@link MosquitoZbrSpawns} 的写法：不进自然生成表（精英怪野外随机刷 = 灾难），
 * 而是按游戏日历每 {@code intervalDays} 天在某个生存玩家附近
 * {@code minRadius~maxRadius} 格内召唤一次，并广播提示。
 * <p>
 * SavedData 存累计召唤次数 → 每 200 tick 检查一次 →
 * ① 是否到了下一次该召唤的"游戏日"；② 在某个非创造 / 非观察的生存玩家附近找落脚点；
 * ③ 该半径内已有存活壁虎就跳过（防堆叠）。和平难度 / 角色扮演模式不刷。
 * <p>
 * 默认首次第 12 天、之后每 20 天一次。想关掉把 {@code firstDay} 设 0。
 */
public final class GeckoZbrSpawns {

    private GeckoZbrSpawns() {}

    /** 寻找落脚点的最大尝试次数 */
    private static final int MAX_ATTEMPTS = 24;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 每 200 tick 检查一次（=10 秒），降低开销
            ServerLevel level = server.overworld();
            if (level.getGameTime() % 200 != 0
                    || level.getDifficulty() == Difficulty.PEACEFUL
                    || RoleplayMode.isEnabled(server)) return;

            var cfg = xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.geckoZbr;
            if (cfg.firstDay <= 0) return; // 0 = 关闭事件召唤
            final int minRadius = Math.max(8, cfg.minRadius);
            final int maxRadius = Math.max(minRadius, cfg.maxRadius);
            final int interval = Math.max(1, cfg.intervalDays);
            final double nearbyCheck = Math.max(16, cfg.nearbyBossCheck);

            List<ServerPlayer> players = level.players().stream()
                    .filter(p -> !p.isSpectator() && !p.isCreative() && p.isAlive()).toList();
            if (players.isEmpty()) return;

            State state = level.getDataStorage().computeIfAbsent(State.TYPE);
            long day = xiaoshi2022.corpseorigin.util.WorldCalendar.dayTime(level) / 24000 + 1;
            if (day < nextDay(state.spawnCount, cfg.firstDay, interval)) return;

            ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
            if (trySpawn(level, target, minRadius, maxRadius, nearbyCheck)) {
                state.spawnCount++;
                state.setDirty();
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("message.corpseorigin.gecko_arrival",
                                target.getDisplayName()), false);
                CorpseOrigin.LOGGER.info("壁虎奇葩尸兄在玩家 {} 附近现身（第 {} 次）",
                        target.getName().getString(), state.spawnCount);
            }
        });
    }

    /** 第 n 次召唤对应的"该出现的游戏日" */
    private static long nextDay(int count, int firstDay, int intervalDays) {
        return firstDay + (long) count * intervalDays;
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player,
                                    int minRadius, int maxRadius, double nearbyCheck) {
        // 附近已有壁虎 → 跳过（防止堆叠）
        if (!level.getEntitiesOfClass(GeckoZbrEntity.class,
                new AABB(player.blockPosition()).inflate(nearbyCheck),
                GeckoZbrEntity::isAlive).isEmpty()) return false;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int x = player.getBlockX() + level.getRandom().nextInt(maxRadius - minRadius + 1) + minRadius
                    * (level.getRandom().nextBoolean() ? 1 : -1);
            int z = player.getBlockZ() + level.getRandom().nextInt(maxRadius - minRadius + 1) + minRadius
                    * (level.getRandom().nextBoolean() ? 1 : -1);
            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            GeckoZbrEntity gecko = ModEntities.GECKO_ZBR.create(level, EntitySpawnReason.EVENT);
            if (gecko == null) return false;
            gecko.setPos(x + 0.5, pos.getY(), z + 0.5);
            if (!level.noCollision(gecko) || !level.getFluidState(pos).isEmpty()) continue;
            return level.addFreshEntity(gecko);
        }
        return false;
    }

    private static final class State extends SavedData {
        private static final Codec<State> CODEC = Codec.INT.xmap(State::new, s -> s.spawnCount);
        private static final SavedDataType<State> TYPE = new SavedDataType<>(
                CorpseOrigin.id("gecko_zbr_spawns"), State::new, CODEC, null);
        private int spawnCount;
        private State() {}
        private State(int spawnCount) { this.spawnCount = spawnCount; }
    }
}
