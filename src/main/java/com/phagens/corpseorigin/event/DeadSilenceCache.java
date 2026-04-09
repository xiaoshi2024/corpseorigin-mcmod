package com.phagens.corpseorigin.event;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class DeadSilenceCache {
    
    private static final Long2BooleanMap chunkHasDeadSilence = new Long2BooleanOpenHashMap();
    private static final LongOpenHashSet deadSilenceBlockPositions = new LongOpenHashSet();
    
    private static final int ZONE_RADIUS = 37;
    
    static {
        chunkHasDeadSilence.defaultReturnValue(false);
    }
    
    public static void addDeadSilenceBlock(ServerLevel level, BlockPos pos) {
        deadSilenceBlockPositions.add(pos.asLong());
        long chunkKey = asChunkKey(pos);
        chunkHasDeadSilence.put(chunkKey, true);
    }
    
    public static void removeDeadSilenceBlock(ServerLevel level, BlockPos pos) {
        deadSilenceBlockPositions.remove(pos.asLong());
        long chunkKey = asChunkKey(pos);
        boolean anyLeft = false;
        for (long key : deadSilenceBlockPositions) {
            BlockPos bp = BlockPos.of(key);
            if (asChunkKey(bp) == chunkKey) {
                anyLeft = true;
                break;
            }
        }
        if (!anyLeft) {
            chunkHasDeadSilence.remove(chunkKey);
        }
    }
    
    public static void addGroupAsDeadSilence(ServerLevel level, LongOpenHashSet group) {
        for (long key : group) {
            deadSilenceBlockPositions.add(key);
            BlockPos pos = BlockPos.of(key);
            chunkHasDeadSilence.put(asChunkKey(pos), true);
        }
    }
    
    public static void removeGroupDeadSilence(ServerLevel level, LongOpenHashSet group) {
        for (long key : group) {
            deadSilenceBlockPositions.remove(key);
        }
        for (long key : group) {
            BlockPos pos = BlockPos.of(key);
            long chunkKey = asChunkKey(pos);
            boolean anyLeft = false;
            for (long bk : deadSilenceBlockPositions) {
                if (asChunkKey(BlockPos.of(bk)) == chunkKey) {
                    anyLeft = true;
                    break;
                }
            }
            if (!anyLeft) {
                chunkHasDeadSilence.remove(chunkKey);
            }
        }
    }
    
    public static boolean isInDeadSilenceZone(BlockPos pos) {
        if (deadSilenceBlockPositions.isEmpty()) return false;
        
        for (long key : deadSilenceBlockPositions) {
            BlockPos bp = BlockPos.of(key);
            double distSq = pos.distToCenterSqr(bp.getX(), bp.getY(), bp.getZ());
            if (distSq <= (long) ZONE_RADIUS * ZONE_RADIUS) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isNearDeadSilenceBlock(BlockPos pos, int radius) {
        if (deadSilenceBlockPositions.isEmpty()) return false;
        
        long radiusSq = (long) radius * radius;
        for (long key : deadSilenceBlockPositions) {
            BlockPos bp = BlockPos.of(key);
            double distSq = pos.distToCenterSqr(bp.getX(), bp.getY(), bp.getZ());
            if (distSq <= radiusSq) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasDeadSilenceBlocks() {
        return !deadSilenceBlockPositions.isEmpty();
    }
    
    public static void clear() {
        deadSilenceBlockPositions.clear();
        chunkHasDeadSilence.clear();
    }
    
    private static long asChunkKey(BlockPos pos) {
        return (long) (pos.getX() >> 4) << 32 | (pos.getZ() >> 4) & 0xFFFFFFFFL;
    }
}
