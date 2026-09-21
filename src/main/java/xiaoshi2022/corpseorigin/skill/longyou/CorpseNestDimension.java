package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;
import xiaoshi2022.corpseorigin.registry.ModBlocks;

import java.util.Set;

public final class CorpseNestDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(
            Registries.DIMENSION, CorpseOrigin.id("corpse_nest"));
    private static final BlockPos FLOOR = new BlockPos(0, 64, 0);
    private static final int FLAT_VERSION = 5;

    private CorpseNestDimension() {}

    public static void enter(ServerPlayer player) {
        if (player.level().dimension().equals(KEY)) {
            returnToSpawn(player);
            return;
        }
        ServerLevel target = player.level().getServer().getLevel(KEY);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("dimension.corpseorigin.corpse_nest.missing"));
            return;
        }
        migrateOldChamber(target);
        // Existing void chunks need a landing surface; preserve player builds.
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            BlockPos pos = FLOOR.offset(x, 0, z);
            if (target.getBlockState(pos).isAir()) {
                target.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        if (!HCityEntrance.ensureGenerated(target)) {
            player.sendOverlayMessage(Component.translatable("dimension.corpseorigin.h_city.failed"));
            return;
        }
        Vec3 arrival = HCityEntrance.findArrival(target, player);
        float yaw = (float) Math.toDegrees(Math.atan2(arrival.x - .5, .5 - arrival.z));
        player.teleportTo(target, arrival.x, arrival.y, arrival.z,
                Set.of(), yaw, 0, false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
    }

    private static void returnToSpawn(ServerPlayer player) {
        var config = player.getRespawnConfig();
        // Resolve a valid personal Overworld spawn without consuming anchor charges.
        if (config != null && config.respawnData().dimension().equals(Level.OVERWORLD)) {
            var transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            if (transition.newLevel().dimension().equals(Level.OVERWORLD)) {
                Vec3 pos = transition.position();
                player.teleportTo(transition.newLevel(), pos.x, pos.y, pos.z,
                        Set.of(), transition.yRot(), transition.xRot(), false);
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0;
                return;
            }
        }
        ServerLevel overworld = player.level().getServer().overworld();
        var spawn = overworld.getRespawnData();
        BlockPos pos = player.adjustSpawnLocation(overworld, spawn.pos());
        player.teleportTo(overworld, pos.getX() + .5, pos.getY(), pos.getZ() + .5,
                Set.of(), spawn.yaw(), spawn.pitch(), false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
    }

    private static void migrateOldChamber(ServerLevel level) {
        if (!(level.getBlockEntity(FLOOR) instanceof ZBRFleshBlockEntity marker)
                || !marker.isCorpseNestGateway() || marker.getStructureVersion() >= FLAT_VERSION) return;
        // Limit migration to flesh in the known old chamber footprint.
        for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
            for (int y = 65; y <= 88; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).is(ModBlocks.ZBR_FLESH)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            BlockPos ground = new BlockPos(x, 64, z);
            if (!ground.equals(FLOOR) && level.getBlockState(ground).is(ModBlocks.ZBR_FLESH)) {
                level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        marker.setStructureVersion(FLAT_VERSION);
        level.setBlock(FLOOR, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
    }
}
