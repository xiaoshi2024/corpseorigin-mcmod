package com.phagens.corpseorigin.GongFU.Event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.SyncGongFuContainerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 功法客户端事件处理器
 * 处理客户端登录时同步功法数据到服务器
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class GongFuClientEventHandler {

    private static final String CONTAINER_KEY = "GongFuContainer";

    /**
     * 从客户端本地玩家数据读取功法容器
     */
    private static NonNullList<ItemStack> getClientGongFuItems(LocalPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag containerData = playerData.getCompound(CONTAINER_KEY);

        // 根据实际解锁槽位数创建列表
        int slotCount = com.phagens.corpseorigin.GongFU.GongFaZL.SlotConfigManager.getUnlockedSlotCount(player);
        NonNullList<ItemStack> items = NonNullList.withSize(slotCount, ItemStack.EMPTY);


        if (!containerData.isEmpty()) {
            ContainerHelper.loadAllItems(containerData, items, player.registryAccess());
        }

        return items;
    }

    /**
     * 玩家登录服务器事件 - 同步功法容器数据到服务器
     * 解决首次加入服务器时没有功法数据的问题
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (event.getPlayer() == null) return;

        // 延迟发送，确保连接完全建立
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒确保连接稳定
            } catch (InterruptedException e) {
                // ignore
            }

            Minecraft.getInstance().execute(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;

                // 获取客户端本地保存的功法容器数据
                NonNullList<ItemStack> gongFuItems = getClientGongFuItems(player);

                // 检查是否有功法物品
                boolean hasItems = false;
                for (ItemStack stack : gongFuItems) {
                    if (!stack.isEmpty()) {
                        hasItems = true;
                        break;
                    }
                }

                if (hasItems) {
                    // 发送同步包到服务器
                    SyncGongFuContainerPacket packet = SyncGongFuContainerPacket.create(gongFuItems);
                    PacketDistributor.sendToServer(packet);

                    CorpseOrigin.LOGGER.info("【客户端】登录时发送功法容器数据到服务器，共 {} 个物品",
                            gongFuItems.stream().filter(s -> !s.isEmpty()).count());
                } else {
                    CorpseOrigin.LOGGER.debug("【客户端】没有功法数据需要同步");
                }
            });
        }).start();
    }
}
