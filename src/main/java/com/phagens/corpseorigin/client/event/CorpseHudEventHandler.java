package com.phagens.corpseorigin.client.event;

import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "corpseorigin", value = Dist.CLIENT)
public class CorpseHudEventHandler {

    private static final int HEARTS_PER_ROW = 10;
    private static final int ROW_HEIGHT = 10;
    private static final int BASE_HUNGER_OFFSET = 39;
    private static final int HUNGER_BAR_HEIGHT = 10;
    private static final int TEXT_HEIGHT = 10;
    private static final int EXTRA_PADDING = 2;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        renderCorpseHungerHud(event.getGuiGraphics());
    }

    private static void renderCorpseHungerHud(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) return;

        if (!PlayerCorpseData.isCorpse(player)) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 - 91;

        double maxHealth = 20.0;
        try {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) {
                maxHealth = attr.getValue();
            }
        } catch (Exception ignored) {
        }

        int healthRows = (int) Math.ceil(maxHealth / (HEARTS_PER_ROW * 2.0));
        int extraRows = Math.max(0, healthRows - 1);
        int rowOffset = extraRows * ROW_HEIGHT;

        int baseY = screenHeight - BASE_HUNGER_OFFSET;
        int adjustedY = baseY - rowOffset;

        int armorValue = player.getArmorValue();
        if (armorValue > 0) {
            adjustedY -= ROW_HEIGHT;
        }

        int totalHungerHeight = TEXT_HEIGHT + EXTRA_PADDING + HUNGER_BAR_HEIGHT;
        adjustedY = Math.max(totalHungerHeight + 5, adjustedY);

        int hunger = PlayerCorpseData.getHunger(player);
        int maxHunger = 100;

        guiGraphics.fill(x, adjustedY - HUNGER_BAR_HEIGHT, x + 80, adjustedY - 1, 0x40000000);

        int hungerBarWidth = (int) ((float) hunger / maxHunger * 80);
        guiGraphics.fill(x, adjustedY - HUNGER_BAR_HEIGHT, x + hungerBarWidth, adjustedY - 1, 0xFF00FF00);

        guiGraphics.drawString(minecraft.font, "饥饿度: " + hunger, x, adjustedY - HUNGER_BAR_HEIGHT - EXTRA_PADDING - TEXT_HEIGHT + 1, 0xFFFFFF);
    }
}