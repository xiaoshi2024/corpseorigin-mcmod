package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.client.gui.LongyouDialogueScreen;
import net.minecraft.client.Minecraft;

import java.util.List;

public class ClientHandler {
    public static void handleDialogueOptionsPacket(String question, List<String> options, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            // 打开龙右对话框GUI并设置选项
            LongyouDialogueScreen screen = new LongyouDialogueScreen();
            screen.setDialogue(question, options);
            Minecraft.getInstance().setScreen(screen);
        });
    }
}
