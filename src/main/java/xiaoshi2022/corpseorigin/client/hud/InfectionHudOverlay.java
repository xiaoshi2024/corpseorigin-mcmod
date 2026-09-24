package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/** Draws the compact status HUD in the top-right corner. */
public final class InfectionHudOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infection_hud");
    private static final Identifier BATTERY_FRAME = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/battery_frame.png");
    private static final Identifier BATTERY_FILL = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/battery_fill.png");
    private static final Identifier BATTERY_TIP = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/battery_tip.png");

    /* 固定常量 */
    private static final int RIGHT_MARGIN = 8;
    private static final int TOP_MARGIN = 8;
    private static final int BATTERY_TIP_WIDTH = 2;
    private static final int INNER_PADDING = 2;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
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

        // ★ 从配置读取动态参数
        CorpseConfig.Hud cfg = CorpseConfig.get().hud;
        float scale = cfg.scale;
        int batteryWidth = Math.round(cfg.batteryWidth * scale);
        int batteryHeight = Math.round(cfg.batteryHeight * scale);
        int rowGap = Math.round(cfg.rowGap * scale);

        // ★ 位置计算：x/y 为 -1 时默认靠右上（坐标不受 scale 影响）
        int width = graphics.guiWidth();
        int x = cfg.x >= 0 ? Math.round(cfg.x * scale) : width - batteryWidth - BATTERY_TIP_WIDTH - RIGHT_MARGIN;
        int y = cfg.y >= 0 ? Math.round(cfg.y * scale) : TOP_MARGIN;

        // 字体高度按 scale 放大（centeredText 用 mc.font，已经是标准大小，我们单独算行距）
        int scaledLineHeight = Math.round(mc.font.lineHeight * scale);

        int level = EvolutionManager.getLevel(ClientState.earnedPoints);
        String tierFullName = EvolutionTier.formatFullName(level);
        int tierColor = EvolutionTier.colorOf(level);
        String evolutionLine = tierFullName + "  |  点数：" + ClientState.availablePoints;
        graphics.centeredText(mc.font, evolutionLine, x + batteryWidth / 2, y, tierColor);
        y += scaledLineHeight + rowGap;

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
        drawBattery(graphics, mc, x, y, infection, 100, batteryWidth, batteryHeight,
                rowGap, infection >= 60 ? INFECTION_HIGH_COLOR : INFECTION_LOW_COLOR,
                Component.translatable("hud.corpseorigin.infection", infection));
        y += batteryHeight + rowGap;

        int maxInnerPower = ClientState.maxInnerPower;
        if (maxInnerPower > 0) {
            int innerPower = clamp(ClientState.innerPower, 0, maxInnerPower);
            drawBattery(graphics, mc, x, y, innerPower, maxInnerPower, batteryWidth, batteryHeight,
                    rowGap, INNER_POWER_COLOR,
                    Component.translatable("hud.corpseorigin.inner_power", innerPower, maxInnerPower));
            y += batteryHeight + rowGap;
        }

        String role = mc.player.getAttachedOrCreate(ChapterActorState.ROLE);
        int blood = clamp(mc.player.getAttachedOrCreate(BloodReserve.VALUE), 0, BLOOD_MAX);
        // 尸王恒显气血条；左护法只有攒下气血（打怪 / 吃尸肉）后才显示，免得平时空条占位置
        if ("longyou".equals(role) || ("zuohufa".equals(role) && blood > 0)) {
            drawBattery(graphics, mc, x, y, blood, BLOOD_MAX, batteryWidth, batteryHeight,
                    rowGap, BLOOD_COLOR,
                    Component.translatable("hud.corpseorigin.blood", blood, BLOOD_MAX));
        }
    }

    private static void drawBattery(GuiGraphicsExtractor graphics, Minecraft mc,
                                    int x, int y, int value, int max,
                                    int batteryWidth, int batteryHeight, int rowGap,
                                    int fillColor, Component label) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BATTERY_FRAME, x, y, 0, 0,
                batteryWidth, batteryHeight, batteryWidth, batteryHeight);

        int innerWidth = batteryWidth - INNER_PADDING * 2;
        int filledWidth = max <= 0 ? 0 : (int) Math.round(innerWidth * value / (double) max);
        if (filledWidth > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, BATTERY_FILL,
                    x + INNER_PADDING, y + INNER_PADDING, 0, 0,
                    filledWidth, batteryHeight - INNER_PADDING * 2,
                    batteryWidth - INNER_PADDING * 2, batteryHeight - INNER_PADDING * 2);

        int tipY = y + batteryHeight / 3;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BATTERY_TIP, x + batteryWidth, tipY,
                0, 0, BATTERY_TIP_WIDTH, batteryHeight - batteryHeight / 3 - batteryHeight / 3,
                BATTERY_TIP_WIDTH, batteryHeight - batteryHeight / 3 - batteryHeight / 3);

        String text = mc.font.plainSubstrByWidth(label.getString(), batteryWidth - 6);
        graphics.centeredText(mc.font, text, x + batteryWidth / 2, y + 2, TEXT_COLOR);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
