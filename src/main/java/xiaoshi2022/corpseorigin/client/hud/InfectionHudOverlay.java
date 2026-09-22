package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/** Draws the compact status HUD in the top-right corner. */
public final class InfectionHudOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infection_hud");

    /*
     * HUD appearance settings. Change these values to adjust the layout later.
     * Colors use ARGB: 0xAARRGGBB.
     */
    private static final int RIGHT_MARGIN = 8;
    private static final int TOP_MARGIN = 8;
    private static final int BATTERY_WIDTH = 116;
    private static final int BATTERY_HEIGHT = 13;
    private static final int BATTERY_TIP_WIDTH = 2;
    private static final int ROW_GAP = 3;
    private static final int INNER_PADDING = 2;

    private static final int BORDER_COLOR = 0xDDE4E8E8;
    private static final int BACKGROUND_COLOR = 0xCC111617;
    private static final int TIP_COLOR = 0xDDB7BDBD;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int EVOLUTION_TEXT_COLOR = 0xFFB8E6B8;
    private static final int INFECTION_LOW_COLOR = 0xFF9B4CB0;
    private static final int INFECTION_HIGH_COLOR = 0xFFD33D55;
    private static final int INNER_POWER_COLOR = 0xFF279FE0;
    private static final int BLOOD_COLOR = 0xFFC92F49;
    private static final int BLOOD_MAX = 600;

    private InfectionHudOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(
                HUD_ID,
                (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> render(graphics));
        CorpseOrigin.LOGGER.debug("[HUD] Compact battery HUD registered");
    }

    private static void render(GuiGraphicsExtractor graphics) {
        if (!ClientState.hudVisible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return;

        int x = graphics.guiWidth() - BATTERY_WIDTH - BATTERY_TIP_WIDTH - RIGHT_MARGIN;
        int y = TOP_MARGIN;

        int level = EvolutionManager.getLevel(ClientState.earnedPoints);
        Component evolution = Component.translatable(
                "hud.corpseorigin.evolution", level, ClientState.availablePoints);
        graphics.centeredText(mc.font, evolution, x + BATTERY_WIDTH / 2, y, EVOLUTION_TEXT_COLOR);
        y += mc.font.lineHeight + ROW_GAP;

        boolean isCorpse = false;
        var selfData = CorpseOriginClient.corpseDataCache.get(mc.player.getUUID());
        if (selfData != null && selfData.isCorpse) {
            isCorpse = true;
        }

        int infection = isCorpse ? 100 : ClientState.infection;
        if (!isCorpse && infection <= 0 && mc.player.hasEffect(ModEffects.QIANS)) {
            infection = 1;
        }
        infection = clamp(infection, 0, 100);
        drawBattery(graphics, mc, x, y, infection, 100,
                infection >= 60 ? INFECTION_HIGH_COLOR : INFECTION_LOW_COLOR,
                Component.translatable("hud.corpseorigin.infection", infection));
        y += BATTERY_HEIGHT + ROW_GAP;

        int maxInnerPower = ClientState.maxInnerPower;
        if (maxInnerPower > 0) {
            int innerPower = clamp(ClientState.innerPower, 0, maxInnerPower);
            drawBattery(graphics, mc, x, y, innerPower, maxInnerPower, INNER_POWER_COLOR,
                    Component.translatable("hud.corpseorigin.inner_power", innerPower, maxInnerPower));
            y += BATTERY_HEIGHT + ROW_GAP;
        }

        if ("longyou".equals(mc.player.getAttachedOrCreate(ChapterActorState.ROLE))) {
            int blood = clamp(mc.player.getAttachedOrCreate(BloodReserve.VALUE), 0, BLOOD_MAX);
            drawBattery(graphics, mc, x, y, blood, BLOOD_MAX, BLOOD_COLOR,
                    Component.translatable("hud.corpseorigin.blood", blood, BLOOD_MAX));
        }
    }

    private static void drawBattery(GuiGraphicsExtractor graphics, Minecraft mc,
                                    int x, int y, int value, int max, int fillColor,
                                    Component label) {
        graphics.fill(x, y, x + BATTERY_WIDTH, y + BATTERY_HEIGHT, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + BATTERY_WIDTH - 1, y + BATTERY_HEIGHT - 1,
                BACKGROUND_COLOR);

        int innerWidth = BATTERY_WIDTH - INNER_PADDING * 2;
        int filledWidth = max <= 0 ? 0 : (int) Math.round(innerWidth * value / (double) max);
        if (filledWidth > 0) {
            graphics.fill(x + INNER_PADDING, y + INNER_PADDING,
                    x + INNER_PADDING + filledWidth, y + BATTERY_HEIGHT - INNER_PADDING,
                    fillColor);
        }

        int tipY = y + BATTERY_HEIGHT / 3;
        graphics.fill(x + BATTERY_WIDTH, tipY,
                x + BATTERY_WIDTH + BATTERY_TIP_WIDTH, y + BATTERY_HEIGHT - BATTERY_HEIGHT / 3,
                TIP_COLOR);

        String text = mc.font.plainSubstrByWidth(label.getString(), BATTERY_WIDTH - 6);
        graphics.centeredText(mc.font, text, x + BATTERY_WIDTH / 2, y + 2, TEXT_COLOR);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
