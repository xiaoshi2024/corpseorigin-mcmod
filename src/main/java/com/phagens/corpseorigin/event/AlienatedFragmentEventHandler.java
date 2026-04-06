package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.custom.AlienatedFragmentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 异化方块事件处理器
 * 监听生物死亡事件，触发异化方块蔓延
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class AlienatedFragmentEventHandler {
    
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        
        if (level.isClientSide) return;
        
        BlockPos deathPos = entity.blockPosition();
        
        BlockPos[] checkPositions = {
            deathPos,
            deathPos.below(),
            deathPos.above()
        };
        
        for (BlockPos checkPos : checkPositions) {
            BlockState state = level.getBlockState(checkPos);
            if (state.getBlock() instanceof AlienatedFragmentBlock block) {
                CorpseOrigin.LOGGER.info("生物 {} 在异化方块上死亡，位置: {}", entity.getName().getString(), checkPos);
                block.onEntityDeath(level, checkPos);
                return;
            }
        }
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    BlockPos checkPos = deathPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof AlienatedFragmentBlock block) {
                        CorpseOrigin.LOGGER.info("生物 {} 在异化方块附近死亡，位置: {}", entity.getName().getString(), checkPos);
                        block.onEntityDeath(level, checkPos);
                        return;
                    }
                }
            }
        }
    }
}
