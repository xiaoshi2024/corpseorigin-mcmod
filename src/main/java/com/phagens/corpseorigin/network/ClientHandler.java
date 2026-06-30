package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.character.CharacterManager;
import com.phagens.corpseorigin.client.gui.LongyouDialogueScreen;
import com.phagens.corpseorigin.player.CorpsePlayerAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

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

    public static void handleCharacterSync(String characterId, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            CharacterManager.getInstance().setClientCachedCharacter(characterId);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a角色已切换为: " + CharacterManager.getInstance().getPlayerCharacter(mc.player).getName().getString()
                ));
            }
        });
    }

    public static void handlePlayerCorpseSync(int playerId, boolean isCorpse, int corpseType, CompoundTag corpseData, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // 尝试直接获取玩家实体
                Player player = mc.level.getEntity(playerId) instanceof Player p ? p : null;
                if (player != null) {
                    // 直接更新状态
                    player.setData(CorpsePlayerAttachment.IS_CORPSE, isCorpse);
                    player.setData(CorpsePlayerAttachment.CORPSE_TYPE, corpseType);
                    player.setData(CorpsePlayerAttachment.CORPSE_DATA, corpseData);
                } else {
                    // 如果玩家实体还未加载，延迟处理
                    mc.execute(() -> {
                        Player delayedPlayer = mc.level.getEntity(playerId) instanceof Player p ? p : null;
                        if (delayedPlayer != null) {
                            delayedPlayer.setData(CorpsePlayerAttachment.IS_CORPSE, isCorpse);
                            delayedPlayer.setData(CorpsePlayerAttachment.CORPSE_TYPE, corpseType);
                            delayedPlayer.setData(CorpsePlayerAttachment.CORPSE_DATA, corpseData);
                        }
                    });
                }
            }
        });
    }
}
