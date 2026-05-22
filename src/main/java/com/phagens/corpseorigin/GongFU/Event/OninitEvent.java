package com.phagens.corpseorigin.GongFU.Event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.PackGongFu.Paket.OpenGongFuMenuPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;


@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class OninitEvent {
    @SubscribeEvent
    public static void onInventoryScreenInit(ScreenEvent.Init.Post event){
        //检查是否于玩家背包屏幕初始化
        if (event.getScreen() instanceof InventoryScreen inventoryScreen){
            int guiLeft = inventoryScreen.getGuiLeft();
            int guiTop = inventoryScreen.getGuiTop();

            Button button= Button.builder(Component.literal("修行"),
                    button1 -> {
                if (Minecraft.getInstance().player != null){
                    //网络包
                    PacketDistributor.sendToServer(new OpenGongFuMenuPacket());
                }
                    }).bounds(guiLeft + 3, guiTop - 9, 30, 9).build();
            event.addListener(button);
        }
    }



}
