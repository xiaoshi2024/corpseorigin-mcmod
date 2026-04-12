package com.phagens.corpseorigin.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class CorpseBrotherHiveMind {

    private static final int SHARED_TARGET_RANGE_CHUNKS = 9;
    private static final int SHARED_TARGET_RANGE_BLOCKS = SHARED_TARGET_RANGE_CHUNKS * 16;
    private static final int GATHER_SCAN_INTERVAL = 40;
    private static final int GATHER_RANGE = 32;

    public static boolean isCorpseBrother(LivingEntity entity) {
        return entity instanceof ICorpseBrother;
    }

    public static boolean isCorpseBrother(Mob entity) {
        return entity instanceof ICorpseBrother;
    }

    public static void broadcastTarget(ServerLevel level, LivingEntity broadcaster, LivingEntity target) {
        if (target == null || !target.isAlive()) return;

        BlockPos center = broadcaster.blockPosition();
        AABB searchBox = new AABB(
                center.getX() - SHARED_TARGET_RANGE_BLOCKS, center.getY() - SHARED_TARGET_RANGE_BLOCKS, center.getZ() - SHARED_TARGET_RANGE_BLOCKS,
                center.getX() + SHARED_TARGET_RANGE_BLOCKS, center.getY() + SHARED_TARGET_RANGE_BLOCKS, center.getZ() + SHARED_TARGET_RANGE_BLOCKS
        );

        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox,
                mob -> mob != broadcaster
                        && mob.isAlive()
                        && mob instanceof ICorpseBrother
                        && !((ICorpseBrother) mob).hasAttackTarget()
        );

        for (Mob mob : nearbyMobs) {
            ICorpseBrother brother = (ICorpseBrother) mob;
            brother.setHiveMindTarget(target);
        }
    }

    public static LivingEntity findGatherTarget(ServerLevel level, Mob entity) {
        if (entity.tickCount % GATHER_SCAN_INTERVAL != 0) return null;

        BlockPos center = entity.blockPosition();
        AABB searchBox = new AABB(
                center.getX() - GATHER_RANGE, center.getY() - GATHER_RANGE, center.getZ() - GATHER_RANGE,
                center.getX() + GATHER_RANGE, center.getY() + GATHER_RANGE, center.getZ() + GATHER_RANGE
        );

        List<Mob> nearbyBrothers = level.getEntitiesOfClass(Mob.class, searchBox,
                mob -> mob != entity
                        && mob.isAlive()
                        && mob instanceof ICorpseBrother
                        && !((ICorpseBrother) mob).hasAttackTarget()
        );

        if (nearbyBrothers.isEmpty()) return null;

        Mob bestLeader = null;
        int bestLevel = -1;
        for (Mob brother : nearbyBrothers) {
            int evoLevel = ((ICorpseBrother) brother).getEvolutionLevel();
            if (evoLevel > bestLevel) {
                bestLevel = evoLevel;
                bestLeader = brother;
            }
        }

        return bestLeader != null ? bestLeader : nearbyBrothers.get(0);
    }
}
