package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;
import xiaoshi2022.corpseorigin.registry.ModBlocks;

/** A one-time landmark district, including in worlds with already explored void chunks. */
public final class HCityEntrance {
    public static final BlockPos ARRIVAL = new BlockPos(0, 65, 0);
    private static final BlockPos MARKER = new BlockPos(0, 61, 0);
    private static final int VERSION = 100;

    private HCityEntrance() {}

    public static Vec3 findArrival(ServerLevel level, ServerPlayer player) {
        for (BlockPos offset : new BlockPos[]{new BlockPos(0, 0, -24), new BlockPos(24, 0, 0),
                new BlockPos(0, 0, 24), new BlockPos(-24, 0, 0)}) {
            BlockPos feet = ARRIVAL.offset(offset);
            Vec3 pos = Vec3.atBottomCenterOf(feet);
            if (!level.getBlockState(feet.below()).isAir()
                    && level.noCollision(player, player.getBoundingBox().move(pos.subtract(player.position())))) {
                return pos;
            }
        }
        int roof = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        return new Vec3(.5, roof + 1, .5);
    }

    public static boolean ensureGenerated(ServerLevel level) {
        if (level.getBlockEntity(MARKER) instanceof ZBRFleshBlockEntity marker
                && marker.getStructureVersion() >= VERSION) return true;
        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var pool = pools.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,
                CorpseOrigin.id("h_city_library/entrance")));
        // The named front-door connector pins the library doorway to the arrival point
        // regardless of rotation. Vanilla subtracts the pool's one-block ground offset.
        if (!JigsawPlacement.generateJigsaw(level, pool, CorpseOrigin.id("library_arrival"),
                3, ARRIVAL.above(), false)) return false;
        int variant = 0;
        for (int x : new int[]{-80, -16, 48}) for (int z : new int[]{-80, -16, 48}) {
            if (x == -16 || z == -16) continue;
            var template = level.getStructureManager().get(
                    CorpseOrigin.id("h_city_ruins/ruin_" + variant++));
            if (template.isPresent()) {
                BlockPos origin = new BlockPos(x, 64, z);
                template.get().placeInWorld(level, origin, origin,
                        new StructurePlaceSettings(), level.getRandom(), 2);
            }
        }
        // Connect the landmark and ruined blocks, providing support in legacy void chunks.
        for (int x = -84; x <= 84; x++) for (int z = -84; z <= 84; z++) {
            BlockPos pos = new BlockPos(x, 64, z);
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        level.setBlock(MARKER, ModBlocks.ZBR_FLESH.defaultBlockState(), 2);
        if (level.getBlockEntity(MARKER) instanceof ZBRFleshBlockEntity marker) {
            marker.setStructureVersion(VERSION);
        }
        return true;
    }
}
