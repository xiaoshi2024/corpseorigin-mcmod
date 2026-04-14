package com.phagens.corpseorigin.worldgen;

import com.phagens.corpseorigin.block.custom.AlienatedFragmentBlock;
import com.phagens.corpseorigin.register.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AlienatedFragmentFeature extends Feature<NoneFeatureConfiguration> {

    public AlienatedFragmentFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int patchRadius = 8 + random.nextInt(12);
        int patchHeight = 2 + random.nextInt(3);
        int density = 3 + random.nextInt(5);

        boolean placed = false;

        for (int x = -patchRadius; x <= patchRadius; x += density) {
            for (int z = -patchRadius; z <= patchRadius; z += density) {
                double distSq = x * x + z * z;
                if (distSq > patchRadius * patchRadius) continue;

                if (random.nextFloat() > 0.6F) continue;

                int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        origin.getX() + x, origin.getZ() + z);

                if (surfaceY <= level.getMinBuildHeight()) continue;

                BlockPos surfacePos = new BlockPos(origin.getX() + x, surfaceY - 1, origin.getZ() + z);
                BlockState surfaceState = level.getBlockState(surfacePos);

                if (surfaceState.isAir() || !surfaceState.isSolid()) continue;

                int depth = 1 + random.nextInt(patchHeight);
                for (int y = 0; y < depth; y++) {
                    BlockPos placePos = surfacePos.above(y);
                    BlockState targetState = level.getBlockState(placePos);

                    if (!targetState.isAir() && !targetState.getFluidState().isEmpty()) continue;

                    BlockState fragmentState = BlockRegistry.ALIENATED_FRAGMENT.get().defaultBlockState()
                            .setValue(AlienatedFragmentBlock.DEAD_SILENCE, true)
                            .setValue(AlienatedFragmentBlock.DOWN_DEPTH, y > 0 ? Math.min(y, 2) : 0);

                    level.setBlock(placePos, fragmentState, 2 | 16);
                    placed = true;
                }
            }
        }

        return placed;
    }
}
