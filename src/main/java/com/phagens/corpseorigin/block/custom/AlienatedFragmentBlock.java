package com.phagens.corpseorigin.block.custom;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.event.DeadSilenceCache;
import com.phagens.corpseorigin.register.BlockRegistry;
import com.phagens.corpseorigin.register.Moditems;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

public class AlienatedFragmentBlock extends Block {
    
    public static final BooleanProperty DEAD_SILENCE = BooleanProperty.create("dead_silence");
    public static final IntegerProperty DOWN_DEPTH = IntegerProperty.create("down_depth", 0, 2);
    private static final int SPREAD_THRESHOLD = 36;
    private static final int MAX_GROUP_SIZE = 128;
    private static final int MAX_DOWN_SPREAD = 2;
    
    private static final ThreadLocal<Boolean> isUpdating = ThreadLocal.withInitial(() -> false);
    
    public AlienatedFragmentBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.1f, 0.5f)
                .sound(SoundType.STONE)
                .mapColor(MapColor.COLOR_BROWN)
                .randomTicks()
        );
        this.registerDefaultState(this.defaultBlockState().setValue(DEAD_SILENCE, false).setValue(DOWN_DEPTH, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEAD_SILENCE, DOWN_DEPTH);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(DEAD_SILENCE, false).setValue(DOWN_DEPTH, 0);
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        
        if (level.isClientSide) return;
        if (oldState.getBlock() == this) return;
        if (isUpdating.get()) return;
        
        try {
            isUpdating.set(true);
            checkAndUpdateDeadSilence((ServerLevel) level, pos);
        } finally {
            isUpdating.set(false);
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        
        if (level.isClientSide) return;
        if (state.is(newState.getBlock())) return;
        if (isUpdating.get()) return;
        
        boolean wasDeadSilence = state.getValue(DEAD_SILENCE);
        
        try {
            isUpdating.set(true);
            if (wasDeadSilence) {
                DeadSilenceCache.removeDeadSilenceBlock((ServerLevel) level, pos);
            }
            LongOpenHashSet connectedGroup = findConnectedGroupOptimized(level, pos);
            updateDeadSilenceForGroup((ServerLevel) level, connectedGroup);
        } finally {
            isUpdating.set(false);
        }
    }
    
    public void onEntityDeath(Level level, BlockPos deathPos) {
        if (level.isClientSide) return;
        if (isUpdating.get()) return;
        
        try {
            isUpdating.set(true);
            
            LongOpenHashSet connectedGroup = findConnectedGroupOptimized(level, deathPos);
            
            if (connectedGroup.isEmpty()) return;
            
            LongOpenHashSet edgeBlocks = findEdgeBlocksOptimized(connectedGroup);
            
            int spreadCount = spreadBlocksOptimized((ServerLevel) level, edgeBlocks, connectedGroup);
            
            if (spreadCount > 0) {
                connectedGroup = findConnectedGroupOptimized(level, deathPos);
            }
            
            updateDeadSilenceForGroup((ServerLevel) level, connectedGroup);
        } finally {
            isUpdating.set(false);
        }
    }
    
    private LongOpenHashSet findConnectedGroupOptimized(Level level, BlockPos startPos) {
        LongOpenHashSet visited = new LongOpenHashSet();
        long[] queue = new long[MAX_GROUP_SIZE];
        int head = 0, tail = 0;
        
        long startKey = startPos.asLong();
        BlockState startState = level.getBlockState(startPos);
        
        if (startState.getBlock() instanceof AlienatedFragmentBlock) {
            queue[tail++] = startKey;
        } else {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = startPos.relative(dir);
                if (level.getBlockState(neighbor).getBlock() instanceof AlienatedFragmentBlock) {
                    queue[tail++] = neighbor.asLong();
                    break;
                }
            }
        }
        
        while (head < tail && visited.size() < MAX_GROUP_SIZE) {
            long currentKey = queue[head++];
            
            if (visited.contains(currentKey)) continue;
            
            BlockPos current = BlockPos.of(currentKey);
            BlockState state = level.getBlockState(current);
            
            if (state.getBlock() instanceof AlienatedFragmentBlock) {
                visited.add(currentKey);
                
                for (Direction dir : Direction.values()) {
                    long neighborKey = current.relative(dir).asLong();
                    if (!visited.contains(neighborKey) && tail < MAX_GROUP_SIZE) {
                        queue[tail++] = neighborKey;
                    }
                }
            }
        }
        
        return visited;
    }
    
    private LongOpenHashSet findEdgeBlocksOptimized(LongOpenHashSet group) {
        LongOpenHashSet edgeBlocks = new LongOpenHashSet();
        
        for (long key : group) {
            BlockPos pos = BlockPos.of(key);
            for (Direction dir : Direction.values()) {
                long neighborKey = pos.relative(dir).asLong();
                if (!group.contains(neighborKey)) {
                    edgeBlocks.add(key);
                    break;
                }
            }
        }
        
        return edgeBlocks;
    }
    
    private int spreadBlocksOptimized(ServerLevel level, LongOpenHashSet edgeBlocks, LongOpenHashSet existingGroup) {
        int spreadCount = 0;
        
        for (long edgeKey : edgeBlocks) {
            BlockPos edgePos = BlockPos.of(edgeKey);
            BlockState edgeState = level.getBlockState(edgePos);
            int currentDepth = edgeState.getBlock() instanceof AlienatedFragmentBlock ? 
                    edgeState.getValue(DOWN_DEPTH) : 0;
            
            for (Direction dir : Direction.values()) {
                BlockPos spreadPos = edgePos.relative(dir);
                long spreadKey = spreadPos.asLong();
                
                if (existingGroup.contains(spreadKey)) continue;
                
                BlockState targetState = level.getBlockState(spreadPos);
                
                if (targetState.isAir()) continue;
                
                if (!targetState.getFluidState().isEmpty()) continue;
                
                int newDepth = currentDepth;
                if (dir == Direction.DOWN) {
                    if (currentDepth >= MAX_DOWN_SPREAD) {
                        continue;
                    }
                    newDepth = currentDepth + 1;
                }
                
                BlockState spreadState = BlockRegistry.ALIENATED_FRAGMENT.get().defaultBlockState()
                        .setValue(DOWN_DEPTH, newDepth);
                
                level.setBlock(spreadPos, spreadState, 2 | 16);
                spreadCount++;
                
                if (level.random.nextDouble() < 0.15 && !hasCorpseAt(level, spreadPos)) {
                    spawnCorpseGib(level, spreadPos);
                }
            }
        }
        
        return spreadCount;
    }
    
    private boolean hasCorpseAt(ServerLevel level, BlockPos pos) {
        net.minecraft.world.phys.AABB checkBox = new net.minecraft.world.phys.AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1
        );
        return !level.getEntitiesOfClass(com.phagens.corpseorigin.entity.CorpseGibEntity.class, checkBox).isEmpty();
    }
    
    private void spawnCorpseGib(ServerLevel level, BlockPos pos) {
        com.phagens.corpseorigin.entity.CorpseGibEntity corpse = com.phagens.corpseorigin.register.EntityRegistry.CORPSE_GIB.get().create(level);
        if (corpse != null) {
            int randomType = level.random.nextInt(6);
            corpse.setGibType(randomType);
            corpse.setParentType("corpseorigin:alienated_fragment");
            
            corpse.moveTo(
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                level.random.nextFloat() * 360F,
                0.0F
            );
            
            corpse.pitchSpin = (level.random.nextFloat() - 0.5F) * 30F;
            corpse.yawSpin = (level.random.nextFloat() - 0.5F) * 30F;
            
            level.addFreshEntity(corpse);
        }
    }
    
    private void checkAndUpdateDeadSilence(ServerLevel level, BlockPos pos) {
        LongOpenHashSet group = findConnectedGroupOptimized(level, pos);
        updateDeadSilenceForGroup(level, group);
    }
    
    private void updateDeadSilenceForGroup(ServerLevel level, LongOpenHashSet group) {
        boolean shouldBeDeadSilence = group.size() >= SPREAD_THRESHOLD;
        
        if (shouldBeDeadSilence) {
            CorpseOrigin.LOGGER.info("异化方块连接数量达到 {}，形成死寂生物群系！", group.size());
            DeadSilenceCache.addGroupAsDeadSilence(level, group);
            applyDeadSilenceEffect(level, group);
        } else {
            boolean wasDeadSilence = false;
            for (long key : group) {
                BlockPos pos = BlockPos.of(key);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof AlienatedFragmentBlock && state.getValue(DEAD_SILENCE)) {
                    wasDeadSilence = true;
                    break;
                }
            }
            if (wasDeadSilence) {
                DeadSilenceCache.removeGroupDeadSilence(level, group);
            }
        }
        
        for (long key : group) {
            BlockPos pos = BlockPos.of(key);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof AlienatedFragmentBlock) {
                if (state.getValue(DEAD_SILENCE) != shouldBeDeadSilence) {
                    level.setBlock(pos, state.setValue(DEAD_SILENCE, shouldBeDeadSilence), 2);
                }
            }
        }
    }
    
    private void applyDeadSilenceEffect(ServerLevel level, LongOpenHashSet group) {
        int count = 0;
        int cx = 0, cy = 0, cz = 0;
        
        for (long key : group) {
            BlockPos pos = BlockPos.of(key);
            cx += pos.getX();
            cy += pos.getY();
            cz += pos.getZ();
            count++;
        }
        
        if (count > 0) {
            BlockPos center = new BlockPos(cx / count, cy / count, cz / count);
            
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SOUL,
                center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                20, 2.0, 1.0, 2.0, 0.05
            );
        }
    }
    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DEAD_SILENCE)) {
            if (random.nextDouble() < 0.02) {
                level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.01
                );
            }
        }
    }
    
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Moditems.ALIENATED_FRAGMENT_ITEM.get());
    }
}
