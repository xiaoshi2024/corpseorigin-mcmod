package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;
import xiaoshi2022.corpseorigin.registry.ModBlocks;

import java.util.Set;
import java.util.ArrayDeque;

/** 成熟尸巢的内部维度与入口房间。 */
public final class CorpseNestDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(
            Registries.DIMENSION, CorpseOrigin.id("corpse_nest"));
    private static final BlockPos ARRIVAL = new BlockPos(0, 65, 0);
    private static final int STRUCTURE_VERSION = 1;
    private static final int RADIUS = 24;
    private static final int FLOOR_Y = 64;
    private static final int CEILING_Y = 72;
    private static final int CELL_COUNT = 23;

    private CorpseNestDimension() {}

    public static void enter(ServerPlayer player) {
        if (player.level().dimension().equals(KEY)) {
            ServerLevel overworld = player.level().getServer().overworld();
            int y = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0);
            player.teleportTo(overworld, .5, y + 1, .5,
                    Set.of(), player.getYRot(), player.getXRot(), false);
            return;
        }
        ServerLevel target = player.level().getServer().getLevel(KEY);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("dimension.corpseorigin.corpse_nest.missing"));
            return;
        }
        ensureChamber(target);
        player.teleportTo(target, ARRIVAL.getX() + .5, ARRIVAL.getY(), ARRIVAL.getZ() + .5,
                Set.of(), player.getYRot(), player.getXRot(), false);
    }

    private static void ensureChamber(ServerLevel level) {
        BlockPos marker = ARRIVAL.below();
        if (level.getBlockState(marker).is(ModBlocks.ZBR_FLESH)
                && level.getBlockEntity(marker) instanceof ZBRFleshBlockEntity flesh
                && flesh.isCorpseNestGateway()
                && flesh.getStructureVersion() >= STRUCTURE_VERSION) return;

        boolean[][] passages = generateMaze(level.getSeed());
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                level.setBlock(new BlockPos(x, FLOOR_Y, z), ModBlocks.ZBR_FLESH.defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, CEILING_Y, z), ModBlocks.ZBR_FLESH.defaultBlockState(), 2);
                boolean passage = passages[x + RADIUS][z + RADIUS];
                for (int y = FLOOR_Y + 1; y < CEILING_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), passage
                            ? Blocks.AIR.defaultBlockState()
                            : ModBlocks.ZBR_FLESH.defaultBlockState(), 2);
                }
            }
        }

        // Remove the high ceiling left by the original chamber in existing worlds.
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int y = CEILING_Y + 1; y <= 88; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(marker, ModBlocks.ZBR_FLESH.defaultBlockState(), 3);
        if (level.getBlockEntity(marker) instanceof ZBRFleshBlockEntity flesh) {
            flesh.setCorpseNestGateway(true);
            flesh.setStructureVersion(STRUCTURE_VERSION);
        }
        level.setBlock(ARRIVAL, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ARRIVAL.above(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean[][] generateMaze(long seed) {
        int diameter = RADIUS * 2 + 1;
        boolean[][] passages = new boolean[diameter][diameter];
        boolean[][] visited = new boolean[CELL_COUNT][CELL_COUNT];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        RandomSource random = RandomSource.create(seed ^ 0x434F525053454E45L);
        int center = CELL_COUNT / 2;
        visited[center][center] = true;
        carveCell(passages, center, center);
        stack.push(new int[]{center, center});

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            for (int i = directions.length - 1; i > 0; i--) {
                int swap = random.nextInt(i + 1);
                int[] temp = directions[i];
                directions[i] = directions[swap];
                directions[swap] = temp;
            }

            boolean advanced = false;
            for (int[] direction : directions) {
                int nextX = current[0] + direction[0];
                int nextZ = current[1] + direction[1];
                if (nextX < 0 || nextZ < 0 || nextX >= CELL_COUNT || nextZ >= CELL_COUNT
                        || visited[nextX][nextZ]) continue;
                visited[nextX][nextZ] = true;
                carveCell(passages, nextX, nextZ);
                int worldX = -22 + current[0] * 2 + direction[0];
                int worldZ = -22 + current[1] * 2 + direction[1];
                passages[worldX + RADIUS][worldZ + RADIUS] = true;
                stack.push(new int[]{nextX, nextZ});
                advanced = true;
                break;
            }
            if (!advanced) stack.pop();
        }
        return passages;
    }

    private static void carveCell(boolean[][] passages, int cellX, int cellZ) {
        int worldX = -22 + cellX * 2;
        int worldZ = -22 + cellZ * 2;
        passages[worldX + RADIUS][worldZ + RADIUS] = true;
    }
}
