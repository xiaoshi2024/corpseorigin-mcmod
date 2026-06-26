package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.data.InfectionData;
import com.phagens.corpseorigin.register.BiomeRegistry;
import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class BucketEmptyEventHandler {
    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.isCanceled()) {
            return;
        }

        ItemStack heldItem = event.getItemStack();
        if (heldItem.getItem() == Moditems.BYWATER_BUCKET.get() && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
            BlockPos pos = event.getPos().relative(event.getFace());
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                // 标记水源为感染状态
                InfectionData.markWaterInfectedStatic(serverLevel, pos);
                
                // 如果不在死寂群系中，也可以考虑替换群系
                // 但考虑到性能和设计，我们只标记水源感染，让事件处理器通过 InfectionData 检查
                CorpseOrigin.LOGGER.info("尸水桶放置，标记位置 {} 为感染水源", pos);
            }
        }
    }
}