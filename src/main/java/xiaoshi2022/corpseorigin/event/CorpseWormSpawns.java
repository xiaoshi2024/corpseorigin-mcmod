package xiaoshi2022.corpseorigin.event;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
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
import xiaoshi2022.corpseorigin.entity.MultiHeadCorpseWormEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.util.WorldCalendar;

import java.util.List;

/** One persistent, world-wide encounter schedule; failed placements are retried later. */
public final class CorpseWormSpawns {
    private CorpseWormSpawns() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel level = server.overworld();
            if (level.getGameTime() % 200 != 0 || level.getDifficulty() == Difficulty.PEACEFUL) return;
            CorpseConfig.Spawn config = CorpseConfig.get().spawn;
            if (config.corpseWormFirstDay <= 0) return;
            List<ServerPlayer> players = level.players().stream()
                    .filter(player -> !player.isSpectator() && !player.isCreative() && player.isAlive()).toList();
            if (players.isEmpty()) return;
            State state = level.getDataStorage().computeIfAbsent(State.TYPE);
            long day = WorldCalendar.dayTime(level) / 24000 + 1;
            if (day < nextDay(state.spawnCount, config)) return;
            ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
            if (trySpawn(level, player)) {
                state.spawnCount++;
                state.setDirty();
            }
        });
    }

    private static long nextDay(int count, CorpseConfig.Spawn config) {
        long n = count;
        return config.corpseWormFirstDay + n * config.corpseWormBaseIntervalDays
                + n * (n + 1) / 2 * config.corpseWormIntervalIncreaseDays;
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        if (!level.getEntitiesOfClass(MultiHeadCorpseWormEntity.class,
                new AABB(player.blockPosition()).inflate(128), MultiHeadCorpseWormEntity::isAlive).isEmpty()) return false;
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = player.getBlockX() + level.getRandom().nextInt(97) - 48;
            int z = player.getBlockZ() + level.getRandom().nextInt(97) - 48;
            if (Math.abs(x - player.getBlockX()) < 24 && Math.abs(z - player.getBlockZ()) < 24) continue;
            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
            MultiHeadCorpseWormEntity worm = ModEntities.MULTI_HEAD_CORPSE_WORM.create(level, EntitySpawnReason.EVENT);
            if (worm == null) return false;
            worm.setPos(x + .5, pos.getY(), z + .5);
            if (!level.noCollision(worm) || !level.getFluidState(pos).isEmpty()) continue;
            return level.addFreshEntity(worm);
        }
        return false;
    }

    private static final class State extends SavedData {
        private static final Codec<State> CODEC = Codec.INT.xmap(State::new, state -> state.spawnCount);
        private static final SavedDataType<State> TYPE = new SavedDataType<>(
                CorpseOrigin.id("corpse_worm_spawns"), State::new, CODEC, null);
        private int spawnCount;
        private State() {}
        private State(int spawnCount) { this.spawnCount = spawnCount; }
    }
}
