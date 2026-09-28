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
import xiaoshi2022.corpseorigin.character.CharacterManager;

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
    private static final int BLOOD_MAX = BloodReserve.MAX;

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

        int level = ClientState.evolutionLevel;
        String tierFullName = EvolutionTier.formatFullName(level).getString();
        int tierColor = EvolutionTier.colorOf(level);
        String evolutionLine = tierFullName + net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.098") + compact(ClientState.availablePoints);
        int labelWidth = Math.min(width-8,mc.font.width(evolutionLine));
        int labelX = Math.clamp(x+batteryWidth/2-labelWidth/2,4,Math.max(4,width-labelWidth-4));
        graphics.text(mc.font,mc.font.plainSubstrByWidth(evolutionLine,width-8),labelX,y,tierColor,true);
        y += scaledLineHeight + rowGap;

        boolean isCorpse = false;
        var selfData = CorpseOriginClient.corpseDataCache.get(mc.player.getUUID());
        // The corpse cache can briefly contain the previous body's state after
        // switching saves/bodies. The current role is authoritative for this HUD.
        if (selfData != null && selfData.isCorpse
                && !CharacterManager.getInstance().isMortal(mc.player)) {
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
                    Component.translatable(maxInnerPower>=10000 ? "hud.corpseorigin.inner_power_short" : "hud.corpseorigin.inner_power",
                            compact(innerPower), compact(maxInnerPower)));
            y += batteryHeight + rowGap;
        }

        String role = mc.player.getAttachedOrCreate(ChapterActorState.ROLE);
        int blood = clamp(mc.player.getAttachedOrCreate(BloodReserve.VALUE), 0, BLOOD_MAX);
        // 符合条件即显示空条，让玩家明确知道已经拥有血肉储备。
        if (xiaoshi2022.corpseorigin.skill.longyou.BloodReserveRules.eligible(role, isCorpse, level)
                || ClientState.learnedSkills.stream().anyMatch(path ->
                    xiaoshi2022.corpseorigin.skill.SkillResourceRules.cost(path, 0).blood() > 0)
                || "k".equals(role) || "heixiaofei".equals(role)
                || xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(mc.player,"vampire")
                || isCorpse && (xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(mc.player,"wings")
                || xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(mc.player,"gills"))) {
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
        int filledWidth = max <= 0 ? 0 : (int) Math.round(innerWidth * (double)value / max);
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
    private static String compact(int value) {
        if(value>=1000000000)return String.format(java.util.Locale.ROOT,"%.1fB",value/1e9);
        if(value>=1000000)return String.format(java.util.Locale.ROOT,"%.1fM",value/1e6);
        if(value>=10000)return String.format(java.util.Locale.ROOT,"%.1fk",value/1e3);
        return Integer.toString(value);
    }
}
