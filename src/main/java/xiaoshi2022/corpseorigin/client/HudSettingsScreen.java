package xiaoshi2022.corpseorigin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class HudSettingsScreen extends Screen {

    private static final int PANEL_W_MAX  = 340;
    private static final int ROW_H        = 20;
    private static final int ROW_GAP      = 8;
    private static final int PAD          = 12;
    private static final int LABEL_W      = 90;
    private static final int SLIDER_W     = 170;
    private static final int TOP_Y        = 30;
    private static final int BTN_W        = 90;
    private static final int BTN_H        = 20;
    private static final int BOTTOM_BAR_H = 40;

    // ---- 编辑态 ----
    private int x, y, width, height, rowGap;
    private float scale;
    private int skillX, skillY, skillSpacing;
    private float skillScale;

    // ---- 可滚动内容 ----
    private record ScrollEntry(AbstractWidget widget, int baseX, int baseY) {}
    private final List<ScrollEntry> scrollEntries = new ArrayList<>();

    private record Label(int baseX, int baseY, String key) {}
    private final List<Label> labels = new ArrayList<>();

    // ---- 滚动状态 ----
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int viewTop, viewBottom;

    // ---- 缓存 ----
    private int cachedScreenW, cachedScreenH;
    private int cachedPanelW, cachedPanelLeft, cachedPanelTop, cachedPanelBottom, cachedTitleCx;

    public HudSettingsScreen() {
        super(Component.translatable("gui.corpseorigin.hud_settings.title"));
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        labels.clear();
        scrollEntries.clear();
        scrollOffset = 0;

        int screenW = this.width > 0 ? this.width
                : Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = this.height > 0 ? this.height
                : Minecraft.getInstance().getWindow().getGuiScaledHeight();
        cachedScreenW = screenW;
        cachedScreenH = screenH;

        int panelW = Math.max(240, Math.min(PANEL_W_MAX, screenW - 60));
        cachedPanelW = panelW;

        // ---- 读配置 ----
        CorpseConfig.Hud cfg = CorpseConfig.get().hud;
        x = cfg.x; y = cfg.y;
        width = cfg.batteryWidth; height = cfg.batteryHeight;
        rowGap = cfg.rowGap; scale = cfg.scale;

        CorpseConfig.SkillHud sh = CorpseConfig.get().skillHud;
        skillX = sh.x; skillY = sh.y;
        skillSpacing = sh.spacing; skillScale = sh.scale;

        int cx        = screenW / 2;
        int panelLeft = cx - panelW / 2;
        int labelX    = panelLeft + PAD;
        int sliderX   = labelX + LABEL_W;

        cachedPanelLeft = panelLeft;
        cachedTitleCx   = cx;
        cachedPanelTop  = TOP_Y - 8;

        // ---- 可视区 ----
        int bottomBarY = screenH - BOTTOM_BAR_H;
        viewTop    = TOP_Y + 20;
        viewBottom = Math.max(viewTop + 40, bottomBarY - 8);

        int row = viewTop + 8;

        // ---- HUD 区 ----
        addLabel(labelX, row, "gui.corpseorigin.hud_settings.section_hud");
        row += ROW_H + 2;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.x");
        addSlider(sliderX, row, -1, 1920, x,
                v -> { x = v; },
                () -> x == -1 ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.092") : String.valueOf(x));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.y");
        addSlider(sliderX, row, -1, 1080, y,
                v -> { y = v; },
                () -> y == -1 ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.092") : String.valueOf(y));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.width");
        addSlider(sliderX, row, 40, 400, width,
                v -> { width = v; },
                () -> String.valueOf(width));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.height");
        addSlider(sliderX, row, 6, 60, height,
                v -> { height = v; },
                () -> String.valueOf(height));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.row_gap");
        addSlider(sliderX, row, 0, 20, rowGap,
                v -> { rowGap = v; },
                () -> String.valueOf(rowGap));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.scale");
        addSlider(sliderX, row, 25, 200, Math.round(scale * 100),
                v -> { scale = v / 100F; },
                () -> formatScale(scale));
        row += ROW_H + ROW_GAP + 6;

        // ---- 技能槽区 ----
        addLabel(labelX, row, "gui.corpseorigin.hud_settings.skill_section");
        row += ROW_H + 2;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.skill_x");
        addSlider(sliderX, row, -1, 1920, skillX,
                v -> { skillX = v; },
                () -> skillX == -1 ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.092") : String.valueOf(skillX));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.skill_y");
        addSlider(sliderX, row, -1, 1080, skillY,
                v -> { skillY = v; },
                () -> skillY == -1 ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.092") : String.valueOf(skillY));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.skill_spacing");
        addSlider(sliderX, row, 0, 30, skillSpacing,
                v -> { skillSpacing = v; },
                () -> String.valueOf(skillSpacing));
        row += ROW_H + ROW_GAP;

        addLabel(labelX, row, "gui.corpseorigin.hud_settings.skill_scale");
        addSlider(sliderX, row, 25, 200, Math.round(skillScale * 100),
                v -> { skillScale = v / 100F; },
                () -> formatScale(skillScale));
        row += ROW_H + ROW_GAP + 8;

        contentHeight = row - viewTop;
        applyScroll();

        // ---- 底部按钮（固定） ----
        int bottomGap    = 6;
        int bottomTotalW = BTN_W * 3 + bottomGap * 2;
        int bottomStartX = panelLeft + (panelW - bottomTotalW) / 2;
        int bottomRow    = screenH - BOTTOM_BAR_H + 8;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.hud_settings.reset"),
                        b -> {
                            CorpseConfig.Hud def = new CorpseConfig.Hud();
                            x = def.x; y = def.y;
                            width = def.batteryWidth; height = def.batteryHeight;
                            rowGap = def.rowGap; scale = def.scale;

                            CorpseConfig.SkillHud sdef = new CorpseConfig.SkillHud();
                            skillX = sdef.x; skillY = sdef.y;
                            skillSpacing = sdef.spacing; skillScale = sdef.scale;

                            rebuildWidgets();
                        })
                .bounds(bottomStartX, bottomRow, BTN_W, BTN_H)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.hud_settings.cancel"),
                        b -> onClose())
                .bounds(bottomStartX + BTN_W + bottomGap, bottomRow, BTN_W, BTN_H)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.hud_settings.save"),
                        b -> saveAndExit())
                .bounds(bottomStartX + (BTN_W + bottomGap) * 2, bottomRow, BTN_W, BTN_H)
                .build());

        cachedPanelBottom = screenH - 8;
    }

    // ---- 滚动 ----

    private int maxScroll() {
        return Math.max(0, contentHeight - (viewBottom - viewTop) + 16);
    }

    private void applyScroll() {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        for (ScrollEntry e : scrollEntries) {
            int drawY = e.baseY() - scrollOffset;
            e.widget().setY(drawY);
            boolean visible = drawY + e.widget().getHeight() > viewTop
                    && drawY < viewBottom;
            e.widget().visible = visible;
            e.widget().active  = visible;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < cachedPanelLeft || mouseX > cachedPanelLeft + cachedPanelW) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (maxScroll() <= 0) return false;
        scrollOffset -= (int) (scrollY * 16);
        applyScroll();
        return true;
    }

    // ---- 工厂 ----

    private void addSlider(int x, int y, int min, int max, int initial,
                           IntConsumer setter, Supplier<String> display) {
        AbstractSliderButton slider = new AbstractSliderButton(
                x, y, SLIDER_W, ROW_H,
                Component.literal(display.get()),
                (double) (initial - min) / Math.max(1, max - min)) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(display.get()));
            }

            @Override
            protected void applyValue() {
                int v = (int) Math.round(min + value * (max - min));
                if (min == -1 && v == 0) v = -1;
                setter.accept(v);
                setMessage(Component.literal(display.get()));
            }
        };
        addRenderableWidget(slider);
        scrollEntries.add(new ScrollEntry(slider, x, y));
    }

    private void addLabel(int x, int y, String key) {
        labels.add(new Label(x, y, key));
    }

    // ---- 保存 ----

    private void saveAndExit() {
        CorpseConfig.Hud cfg = CorpseConfig.get().hud;
        cfg.x = x; cfg.y = y;
        cfg.batteryWidth = Math.max(20, width);
        cfg.batteryHeight = Math.max(6, height);
        cfg.rowGap = Math.max(0, rowGap);
        cfg.scale = scale;

        CorpseConfig.SkillHud sh = CorpseConfig.get().skillHud;
        sh.x = skillX; sh.y = skillY;
        sh.spacing = Math.max(0, skillSpacing);
        sh.scale = skillScale;

        CorpseConfig.save();
        onClose();
    }

    private static String formatScale(float s) {
        if (Math.abs(s - 1.0F) < 0.001F) return "1x";
        return String.format("%.2gx", s);
    }

    // ---- 渲染 ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 背景
        graphics.fill(0, 0, cachedScreenW, cachedScreenH, 0x66000000);
        graphics.fill(cachedPanelLeft - 2, cachedPanelTop,
                cachedPanelLeft + cachedPanelW + 2, cachedPanelBottom, 0xCC111111);

        // 标题
        graphics.centeredText(font, getTitle(), cachedTitleCx, TOP_Y, 0xFFFFFFFF);

        // 标签（跟随滚动）
        for (Label l : labels) {
            int drawY = l.baseY() - scrollOffset;
            if (drawY + 12 < viewTop || drawY > viewBottom) continue;
            graphics.text(font, Component.translatable(l.key()),
                    l.baseX(), drawY + 6, 0xFFFFFFFF, false);
        }

        // 滚动条
        int max = maxScroll();
        if (max > 0) {
            int trackX = cachedPanelLeft + cachedPanelW - 6;
            int trackH = viewBottom - viewTop;
            int barH   = Math.max(20, trackH * trackH / contentHeight);
            int barY   = viewTop + (trackH - barH) * scrollOffset / max;
            graphics.fill(trackX, viewTop, trackX + 4, viewBottom, 0x44FFFFFF);
            graphics.fill(trackX, barY, trackX + 4, barY + barH, 0xFFAAAAAA);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}