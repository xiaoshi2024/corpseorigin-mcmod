package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.MissionScrollItem;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class MissionEventHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            CorpseOrigin.LOGGER.info("MissionEventHandler: 玩家 {} 击杀了 {}", player.getName().getString(), event.getEntity().getName().getString());
            CorpseOrigin.LOGGER.info("MissionEventHandler: 玩家是否为尸兄: {}", PlayerCorpseData.isCorpse(player));
            
            if (!PlayerCorpseData.isCorpse(player)) return;

            LivingEntity target = event.getEntity();

            // 检查所有背包物品
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof MissionScrollItem) {
                    String missionType = MissionScrollItem.getMissionType(stack);
                    CorpseOrigin.LOGGER.info("MissionEventHandler: 发现任务纸条，类型: {}, 当前进度: {}/{}", 
                            missionType, MissionScrollItem.getCurrentCount(stack), MissionScrollItem.getTargetCount(stack));

                    boolean shouldIncrement = switch (missionType) {
                        case "kill_villager" -> target instanceof AbstractVillager;
                        case "kill_zombie" -> isZombieType(target);
                        case "kill_any" -> true;
                        case "infect_player" -> false; // 感染任务在 BYeffect 中处理
                        case "collect_item" -> false; // 收集任务在 ItemEntityPickupEvent 中处理
                        default -> false;
                    };
                    
                    CorpseOrigin.LOGGER.info("MissionEventHandler: 是否应该增加进度: {}", shouldIncrement);

                    if (shouldIncrement && !MissionScrollItem.isCompleted(stack)) {
                        MissionScrollItem.incrementCount(stack);
                        CorpseOrigin.LOGGER.info("MissionEventHandler: 进度已更新为: {}", MissionScrollItem.getCurrentCount(stack));

                        if (MissionScrollItem.isCompleted(stack)) {
                            player.sendSystemMessage(Component.translatable("message.corpseorigin.mission_completed"));
                        } else {
                            int remaining = MissionScrollItem.getTargetCount(stack) - MissionScrollItem.getCurrentCount(stack);
                            player.sendSystemMessage(Component.translatable("message.corpseorigin.mission_progress", remaining));
                        }
                    }
                }
            }
        }
    }

    private static boolean isZombieType(LivingEntity entity) {
        String entityTypeName = entity.getType().getDescriptionId();
        return entityTypeName.contains("zombie") ||
               entityTypeName.contains("corpseorigin.lower_level_zb") ||
               entityTypeName.contains("corpseorigin.longyou") ||
               entityTypeName.contains("corpseorigin.guigun");
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        CorpseOrigin.LOGGER.info("MissionEventHandler: 玩家 {} 拾取了物品", player.getName().getString());
        CorpseOrigin.LOGGER.info("MissionEventHandler: 玩家是否为尸兄: {}", PlayerCorpseData.isCorpse(player));
        
        if (!PlayerCorpseData.isCorpse(player)) return;
        if (player.level().isClientSide) return;

        // 检查所有背包物品
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack missionStack = player.getInventory().getItem(i);
            if (missionStack.getItem() instanceof MissionScrollItem) {
                String missionType = MissionScrollItem.getMissionType(missionStack);
                CorpseOrigin.LOGGER.info("MissionEventHandler: 发现收集物品任务纸条，类型: {}, 当前进度: {}/{}", 
                        missionType, MissionScrollItem.getCurrentCount(missionStack), MissionScrollItem.getTargetCount(missionStack));

                if ("collect_item".equals(missionType) && !MissionScrollItem.isCompleted(missionStack)) {
                    MissionScrollItem.incrementCount(missionStack);
                    CorpseOrigin.LOGGER.info("MissionEventHandler: 收集物品任务进度已更新为: {}", MissionScrollItem.getCurrentCount(missionStack));

                    if (MissionScrollItem.isCompleted(missionStack)) {
                        player.sendSystemMessage(Component.translatable("message.corpseorigin.mission_completed"));
                    } else {
                        int remaining = MissionScrollItem.getTargetCount(missionStack) - MissionScrollItem.getCurrentCount(missionStack);
                        player.sendSystemMessage(Component.translatable("message.corpseorigin.mission_progress", remaining));
                    }
                }
            }
        }
    }
}
