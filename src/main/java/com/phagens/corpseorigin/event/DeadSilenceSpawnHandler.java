package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.custom.AlienatedFragmentBlock;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

/**
 * 死寂群系生物生成限制处理器
 * 
 * 功能：
 * 在死寂群系中只允许生成尸兄类怪物
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class DeadSilenceSpawnHandler {

    private static final Set<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> CORPSE_BROTHER_HOLDERS = Set.of(
            EntityRegistry.LOWER_LEVEL_ZB,
            EntityRegistry.ZBR_FISH
    );
    
    private static final int CHECK_RADIUS = 37;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        Level level = mob.level();
        
        if (level.isClientSide) return;
        
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        EntityType<?> entityType = mob.getType();
        
        if (isCorpseBrotherType(entityType)) return;
        
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        
        BlockPos spawnPos = mob.blockPosition();
        
        if (isInDeadSilenceZone(serverLevel, spawnPos)) {
            event.setSpawnCancelled(true);
            CorpseOrigin.LOGGER.debug("阻止非尸兄类怪物 {} 在死寂群系生成", entityType.getDescriptionId());
        }
    }
    
    private static boolean isInDeadSilenceZone(ServerLevel level, BlockPos pos) {
        int step = 8;
        
        for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x += step) {
            for (int y = -16; y <= 16; y += step) {
                for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z += step) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof AlienatedFragmentBlock && 
                        state.getValue(AlienatedFragmentBlock.DEAD_SILENCE)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    public static boolean isCorpseBrotherType(EntityType<?> type) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : CORPSE_BROTHER_HOLDERS) {
            if (holder.get() == type) {
                return true;
            }
        }
        return false;
    }
}
