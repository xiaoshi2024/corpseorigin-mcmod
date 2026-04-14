package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 无智慧尸兄交互限制处理器
 *
 * 【功能说明】
 * 当玩家失去意识成为尸兄后，无法与复杂的方块进行交互
 * 包括：工作台、门、箱子、红石机关等
 *
 * 【限制范围】
 * 1. 容器类：箱子、熔炉、漏斗、发射器等
 * 2. 功能方块：工作台、铁砧、附魔台、酿造台等
 * 3. 红石机关：按钮、拉杆、压力板、红石线等
 * 4. 门窗类：门、活板门、栅栏门
 *
 * 【恢复意识途径】
 * 1. 食用穆博士的眼睛
 * 2. 进化到3级自动恢复
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class MindlessCorpseInteractionHandler {

    /**
     * 阻止无智慧尸兄与方块右键交互
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!PlayerCorpseData.isMindless(player)) {
            return;
        }

        BlockPos pos = event.getPos();
        Block block = event.getLevel().getBlockState(pos).getBlock();

        // 检查是否是受限制的方块
        if (isRestrictedBlock(block)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                sendRestrictionMessage(serverPlayer, block);
            }
        }
    }

    /**
     * 阻止无智慧尸兄使用物品右键
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!PlayerCorpseData.isMindless(player)) {
            return;
        }

        // 检查是否手持需要智慧的物品
        if (isComplexItem(event.getItemStack().getItem())) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "§c§l你无法理解如何使用这个物品..."
                ), true);
            }
        }
    }

    /**
     * 阻止无智慧尸兄与实体交互
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!PlayerCorpseData.isMindless(player)) {
            return;
        }

        // 阻止与村民、商人等交互
        if (event.getTarget() instanceof net.minecraft.world.entity.npc.AbstractVillager ||
            event.getTarget() instanceof net.minecraft.world.entity.animal.horse.AbstractHorse) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "§c§l你无法理解如何与这个生物交流..."
                ), true);
            }
        }
    }

    /**
     * 检查方块是否受限制
     */
    private static boolean isRestrictedBlock(Block block) {
        // 容器类
        if (block instanceof ChestBlock ||
            block instanceof BarrelBlock ||
            block instanceof ShulkerBoxBlock ||
            block instanceof FurnaceBlock ||
            block instanceof BlastFurnaceBlock ||
            block instanceof SmokerBlock ||
            block instanceof HopperBlock ||
            block instanceof DispenserBlock ||
            block instanceof DropperBlock) {
            return true;
        }

        // 功能方块
        if (block instanceof CraftingTableBlock ||
            block instanceof AnvilBlock ||
            block instanceof BrewingStandBlock ||
            block instanceof BeaconBlock) {
            return true;
        }

        // 门窗类
        if (block instanceof DoorBlock ||
            block instanceof TrapDoorBlock ||
            block instanceof FenceGateBlock) {
            return true;
        }

        // 红石机关
        if (block instanceof ButtonBlock ||
            block instanceof LeverBlock ||
            block instanceof ComparatorBlock ||
            block instanceof RepeaterBlock ||
            block instanceof DaylightDetectorBlock ||
            block instanceof ObserverBlock ||
            block instanceof PistonBaseBlock ||
            block instanceof RedStoneWireBlock ||
            block instanceof RedstoneTorchBlock ||
            block instanceof TripWireHookBlock ||
            block instanceof TargetBlock ||
            block instanceof NoteBlock ||
            block instanceof JukeboxBlock) {
            return true;
        }

        // 其他需要智慧的方块
        if (block == Blocks.CRAFTING_TABLE ||
            block == Blocks.ANVIL ||
            block == Blocks.CHIPPED_ANVIL ||
            block == Blocks.DAMAGED_ANVIL ||
            block == Blocks.BREWING_STAND ||
            block == Blocks.BEACON ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.CHEST ||
            block == Blocks.TRAPPED_CHEST ||
            block == Blocks.BARREL ||
            block == Blocks.FURNACE ||
            block == Blocks.BLAST_FURNACE ||
            block == Blocks.SMOKER ||
            block == Blocks.HOPPER ||
            block == Blocks.DROPPER ||
            block == Blocks.DISPENSER ||
            block == Blocks.CRAFTER) {
            return true;
        }

        return false;
    }

    /**
     * 检查物品是否需要智慧才能使用
     */
    private static boolean isComplexItem(net.minecraft.world.item.Item item) {
        // 这里可以添加需要智慧的物品
        // 例如：地图、书、 writable_book 等
        return item instanceof net.minecraft.world.item.MapItem ||
               item instanceof net.minecraft.world.item.WrittenBookItem ||
               item instanceof net.minecraft.world.item.WritableBookItem ||
               item instanceof net.minecraft.world.item.EnchantedBookItem;
    }

    /**
     * 发送限制提示消息
     */
    private static void sendRestrictionMessage(ServerPlayer player, Block block) {
        String message;
        if (block instanceof CraftingTableBlock) {
            message = "§c§l你的双手笨拙地拍打着工作台，却无法理解如何制作...";
        } else if (block instanceof DoorBlock || block instanceof TrapDoorBlock) {
            message = "§c§l你盯着这个机关，却无法理解如何打开它...";
        } else if (block instanceof ChestBlock || block instanceof BarrelBlock) {
            message = "§c§l你试图打开容器，但混沌的意识让你无法理解...";
        } else if (block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof SmokerBlock) {
            message = "§c§l火焰和烟雾让你困惑，你无法操作这个...";
        } else if (block instanceof ButtonBlock || block instanceof LeverBlock) {
            message = "§c§l这个机关对你来说太复杂了...";
        } else {
            message = "§c§l你的意识混沌不清，无法理解如何使用这个...";
        }
        player.sendSystemMessage(Component.literal(message), true);
    }
}
