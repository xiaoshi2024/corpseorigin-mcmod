package xiaoshi2022.corpseorigin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 模组配置总 GUI —— 分页 + 滚动，游戏内直接调参数。
 * <p>
 * 架构（照 {@link HudSettingsScreen} 的惯例）：
 * <ul>
 *   <li>配置用 {@link CorpseConfig#snapshot()} 深拷贝当编辑态，所有控件直接写编辑态对象；</li>
 *   <li>「保存并关闭」→ {@link CorpseConfig#replace} 整体写回 + 落盘；「取消」→ 丢弃编辑态，零脏数据；</li>
 *   <li>字段用数据描述符（{@link Field} 子类）声明，{@link #buildPages()} 里集中定义 —— 加新参数只加一行；</li>
 *   <li>顶部 ←/→ 翻页，内容区可滚动（最多字段页有 8 行，一屏放不下）。</li>
 * </ul>
 * 服务端参数（刷怪等）保存后需 {@code /corpseconfig reload} 或重进存档生效，底部有提示。
 */
public class CorpseConfigScreen extends Screen {

    private static final int PANEL_W_MAX  = 360;
    private static final int ROW_H        = 20;
    private static final int ROW_GAP      = 6;
    private static final int PAD          = 12;
    private static final int LABEL_W      = 130;
    private static final int SLIDER_W     = 180;
    private static final int TOP_Y        = 30;
    private static final int BTN_W        = 88;
    private static final int BTN_H        = 20;
    private static final int BOTTOM_BAR_H = 46;
    private static final int PAGE_BTN_W   = 28;

    /** 编辑态配置（深拷贝），保存时整体写回 */
    private CorpseConfig edited;

    // ---- 页与字段 ----
    private record Page(String titleKey, List<Field> fields) {}
    private final List<Page> pages = new ArrayList<>();
    private int pageIndex = 0;

    // ---- 可滚动内容 ----
    private record ScrollEntry(AbstractWidget widget, int baseX, int baseY) {}
    private final List<ScrollEntry> scrollEntries = new ArrayList<>();
    private record Label(int baseX, int baseY, String key, int color) {}
    private final List<Label> labels = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int viewTop, viewBottom;

    // ---- 缓存 ----
    private int cachedScreenW, cachedScreenH;
    private int cachedPanelW, cachedPanelLeft, cachedPanelTop, cachedPanelBottom, cachedTitleCx;

    public CorpseConfigScreen() {
        super(Component.translatable("gui.corpseorigin.config.title"));
    }

    // ==================== 字段描述符 ====================

    /** 一行 = 字段描述符；reset 供「重置本页」用 */
    private abstract static class Field {
        final String labelKey;
        final Runnable reset;
        Field(String labelKey, Runnable reset) { this.labelKey = labelKey; this.reset = reset; }
        abstract AbstractWidget widget(int x, int y);
    }

    private static final class IntF extends Field {
        final IntSupplier get, def;
        final IntConsumer set;
        final int min, max;
        final Supplier<String> fmt;
        IntF(String key, IntSupplier get, IntConsumer set, IntSupplier def, int min, int max, Supplier<String> fmt) {
            super(key, () -> set.accept(def.getAsInt()));
            this.get = get; this.set = set; this.def = def; this.min = min; this.max = max; this.fmt = fmt;
        }
        @Override AbstractWidget widget(int x, int y) {
            return slider(x, y, min, max, get.getAsInt(), v -> set.accept(v), fmt);
        }
    }

    private static final class FloatF extends Field {
        final DoubleSupplier get, def;
        final DoubleConsumer set;
        final float min, max;
        final Supplier<String> fmt;
        FloatF(String key, DoubleSupplier get, DoubleConsumer set, DoubleSupplier def, float min, float max, Supplier<String> fmt) {
            super(key, () -> set.accept(def.getAsDouble()));
            this.get = get; this.set = set; this.def = def; this.min = min; this.max = max; this.fmt = fmt;
        }
        @Override AbstractWidget widget(int x, int y) {
            // float 走 0~1 比例滑块
            AbstractSliderButton slider = new AbstractSliderButton(
                    x, y, SLIDER_W, ROW_H, Component.literal(fmt.get()),
                    (get.getAsDouble() - min) / (max - min)) {
                @Override protected void updateMessage() { setMessage(Component.literal(fmt.get())); }
                @Override protected void applyValue() {
                    double v = min + value * (max - min);
                    set.accept(v);
                    setMessage(Component.literal(fmt.get()));
                }
            };
            return slider;
        }
    }

    private static final class BoolF extends Field {
        final Supplier<Boolean> get;
        final Consumer<Boolean> set;
        final Supplier<Boolean> def;
        BoolF(String key, Supplier<Boolean> get, Consumer<Boolean> set, Supplier<Boolean> def) {
            super(key, () -> set.accept(def.get()));
            this.get = get; this.set = set; this.def = def;
        }
        @Override AbstractWidget widget(int x, int y) {
            // builder 回调里 b 就是按钮本身，直接翻转 + 刷新文案
            return Button.builder(boolLabel(get.get()),
                            b -> { set.accept(!get.get()); b.setMessage(boolLabel(get.get())); })
                    .bounds(x, y, SLIDER_W, ROW_H).build();
        }
        static Component boolLabel(boolean v) {
            return Component.translatable(v ? "gui.corpseorigin.common.on" : "gui.corpseorigin.common.off");
        }
    }

    /** 动作按钮：点击执行一次性操作（如打开皮肤文件夹），没有"重置"概念 */
    private static final class ActionF extends Field {
        final Supplier<String> buttonText;
        final Runnable action;
        ActionF(String key, Supplier<String> buttonText, Runnable action) {
            super(key, () -> { });
            this.buttonText = buttonText; this.action = action;
        }
        @Override AbstractWidget widget(int x, int y) {
            return Button.builder(Component.translatable(buttonText.get()), b -> action.run())
                    .bounds(x, y, SLIDER_W, ROW_H).build();
        }
    }

    /** 文本输入框：输入后按回车提交（清空复位），没有"重置"概念 */
    private static final class TextF extends Field {
        final Supplier<String> hint;
        final java.util.function.Consumer<String> onSubmit;
        TextF(String key, Supplier<String> hint, java.util.function.Consumer<String> onSubmit) {
            super(key, () -> { });
            this.hint = hint; this.onSubmit = onSubmit;
        }
        @Override AbstractWidget widget(int x, int y) {
            net.minecraft.client.gui.components.EditBox box =
                    new net.minecraft.client.gui.components.EditBox(
                            net.minecraft.client.Minecraft.getInstance().font, x, y, SLIDER_W, ROW_H,
                            Component.translatable(labelKey)) {
                        @Override
                        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
                            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                                    || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                                String value = getValue().trim();
                                if (!value.isEmpty()) {
                                    onSubmit.accept(value);
                                    setValue("");
                                    setHint(Component.translatable(hint.get()));
                                }
                                return true;
                            }
                            return super.keyPressed(event);
                        }
                    };
            box.setMaxLength(16);
            box.setHint(Component.translatable(hint.get()));
            return box;
        }
    }

    /** 只读信息行：展示动态内容（如当前名单概要），点击无效果 */
    private static final class InfoF extends Field {
        final Supplier<String> text;
        InfoF(String key, Supplier<String> text) {
            super(key, () -> { });
            this.text = text;
        }
        @Override AbstractWidget widget(int x, int y) {
            return Button.builder(Component.literal(text.get()), b -> { })
                    .bounds(x, y, SLIDER_W, ROW_H)
                    .build();
        }
    }

    private static AbstractWidget slider(int x, int y, int min, int max, int initial,
                                         IntConsumer setter, Supplier<String> display) {
        return new AbstractSliderButton(x, y, SLIDER_W, ROW_H,
                Component.literal(display.get()),
                (double) (initial - min) / Math.max(1, max - min)) {
            @Override protected void updateMessage() { setMessage(Component.literal(display.get())); }
            @Override protected void applyValue() {
                setter.accept((int) Math.round(min + value * (max - min)));
                setMessage(Component.literal(display.get()));
            }
        };
    }

    // ==================== 页面定义（加参数就加一行） ====================

    private void buildPages() {
        pages.clear();
        CorpseConfig c = edited;
        var spawn = c.spawn;
        var eldor = spawn.eldorKing;
        var ekDef = new CorpseConfig.Spawn.EldorKing();
        var mu = spawn.muDoctor;
        var muDef = new CorpseConfig.Spawn.MuDoctor();
        var spawnDef = new CorpseConfig.Spawn();
        var sv = c.swordVisuals;
        var svDef = new CorpseConfig.SwordVisuals();
        var gi = c.gourdInheritance;
        var giDef = new CorpseConfig.GourdInheritance();
        var realm = c.realm;
        var realmDef = new xiaoshi2022.corpseorigin.growth.RealmConfig();
        var growth = c.growth;
        var growthDef = new xiaoshi2022.corpseorigin.growth.GrowthConfig();
        var horror = c.corpseHorror;
        var horrorDef = new xiaoshi2022.corpseorigin.growth.CorpseHorrorConfig();

        // ---- 第 1 页：尔多兽王降临 ----
        List<Field> eldorFields = List.of(
                new IntF("gui.corpseorigin.config.f.eldor_first_day",
                        () -> eldor.firstDay, v -> eldor.firstDay = v,
                        () -> ekDef.firstDay, 0, 100,
                        () -> eldor.firstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(eldor.firstDay)),
                new IntF("gui.corpseorigin.config.f.eldor_interval",
                        () -> eldor.intervalDays, v -> eldor.intervalDays = v,
                        () -> ekDef.intervalDays, 1, 60, () -> String.valueOf(eldor.intervalDays)),
                new IntF("gui.corpseorigin.config.f.eldor_min_radius",
                        () -> eldor.minRadius, v -> eldor.minRadius = v,
                        () -> ekDef.minRadius, 8, 96, () -> String.valueOf(eldor.minRadius)),
                new IntF("gui.corpseorigin.config.f.eldor_max_radius",
                        () -> eldor.maxRadius, v -> eldor.maxRadius = v,
                        () -> ekDef.maxRadius, 16, 128, () -> String.valueOf(eldor.maxRadius)),
                new IntF("gui.corpseorigin.config.f.eldor_nearby_check",
                        () -> eldor.nearbyBossCheck, v -> eldor.nearbyBossCheck = v,
                        () -> ekDef.nearbyBossCheck, 16, 256, () -> String.valueOf(eldor.nearbyBossCheck)));
        pages.add(new Page("gui.corpseorigin.config.page_eldor", eldorFields));

        // ---- 第 1b 页：穆博士降临 ----
        List<Field> muFields = List.of(
                new IntF("gui.corpseorigin.config.f.mu_first_day",
                        () -> mu.firstDay, v -> mu.firstDay = v,
                        () -> muDef.firstDay, 0, 100,
                        () -> mu.firstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(mu.firstDay)),
                new IntF("gui.corpseorigin.config.f.mu_interval",
                        () -> mu.intervalDays, v -> mu.intervalDays = v,
                        () -> muDef.intervalDays, 1, 60, () -> String.valueOf(mu.intervalDays)),
                new IntF("gui.corpseorigin.config.f.mu_min_radius",
                        () -> mu.minRadius, v -> mu.minRadius = v,
                        () -> muDef.minRadius, 8, 96, () -> String.valueOf(mu.minRadius)),
                new IntF("gui.corpseorigin.config.f.mu_max_radius",
                        () -> mu.maxRadius, v -> mu.maxRadius = v,
                        () -> muDef.maxRadius, 16, 128, () -> String.valueOf(mu.maxRadius)),
                new IntF("gui.corpseorigin.config.f.mu_nearby_check",
                        () -> mu.nearbyBossCheck, v -> mu.nearbyBossCheck = v,
                        () -> muDef.nearbyBossCheck, 16, 256, () -> String.valueOf(mu.nearbyBossCheck)));
        pages.add(new Page("gui.corpseorigin.config.page_mu", muFields));

        // ---- 第 1c 页：蚊子尸兄降临 ----
        var mosquito = spawn.mosquito;
        var moDef = new CorpseConfig.Spawn.Mosquito();
        List<Field> moFields = List.of(
                new IntF("gui.corpseorigin.config.f.mosquito_first_day",
                        () -> mosquito.firstDay, v -> mosquito.firstDay = v,
                        () -> moDef.firstDay, 0, 100,
                        () -> mosquito.firstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(mosquito.firstDay)),
                new IntF("gui.corpseorigin.config.f.mosquito_interval",
                        () -> mosquito.intervalDays, v -> mosquito.intervalDays = v,
                        () -> moDef.intervalDays, 1, 60, () -> String.valueOf(mosquito.intervalDays)),
                new IntF("gui.corpseorigin.config.f.mosquito_min_radius",
                        () -> mosquito.minRadius, v -> mosquito.minRadius = v,
                        () -> moDef.minRadius, 8, 96, () -> String.valueOf(mosquito.minRadius)),
                new IntF("gui.corpseorigin.config.f.mosquito_max_radius",
                        () -> mosquito.maxRadius, v -> mosquito.maxRadius = v,
                        () -> moDef.maxRadius, 16, 128, () -> String.valueOf(mosquito.maxRadius)),
                new IntF("gui.corpseorigin.config.f.mosquito_nearby_check",
                        () -> mosquito.nearbyBossCheck, v -> mosquito.nearbyBossCheck = v,
                        () -> moDef.nearbyBossCheck, 16, 256, () -> String.valueOf(mosquito.nearbyBossCheck)));
        pages.add(new Page("gui.corpseorigin.config.page_mosquito", moFields));

        // ---- 第 1d 页：青蛙奇葩尸兄降临 ----
        var frog = spawn.frogZbrMc;
        var frogDef = new CorpseConfig.Spawn.FrogZbrMc();
        List<Field> frogFields = List.of(
                new IntF("gui.corpseorigin.config.f.frog_first_day",
                        () -> frog.firstDay, v -> frog.firstDay = v,
                        () -> frogDef.firstDay, 0, 100,
                        () -> frog.firstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(frog.firstDay)),
                new IntF("gui.corpseorigin.config.f.frog_interval",
                        () -> frog.intervalDays, v -> frog.intervalDays = v,
                        () -> frogDef.intervalDays, 1, 60, () -> String.valueOf(frog.intervalDays)),
                new IntF("gui.corpseorigin.config.f.frog_min_radius",
                        () -> frog.minRadius, v -> frog.minRadius = v,
                        () -> frogDef.minRadius, 8, 96, () -> String.valueOf(frog.minRadius)),
                new IntF("gui.corpseorigin.config.f.frog_max_radius",
                        () -> frog.maxRadius, v -> frog.maxRadius = v,
                        () -> frogDef.maxRadius, 16, 128, () -> String.valueOf(frog.maxRadius)),
                new IntF("gui.corpseorigin.config.f.frog_nearby_check",
                        () -> frog.nearbyBossCheck, v -> frog.nearbyBossCheck = v,
                        () -> frogDef.nearbyBossCheck, 16, 256, () -> String.valueOf(frog.nearbyBossCheck)));
        pages.add(new Page("gui.corpseorigin.config.page_frog", frogFields));

        // ---- 第 1e 页：壁虎奇葩尸兄降临 ----
        var gecko = spawn.geckoZbr;
        var geckoDef = new CorpseConfig.Spawn.GeckoZbr();
        List<Field> geckoFields = List.of(
                new IntF("gui.corpseorigin.config.f.gecko_first_day",
                        () -> gecko.firstDay, v -> gecko.firstDay = v,
                        () -> geckoDef.firstDay, 0, 100,
                        () -> gecko.firstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(gecko.firstDay)),
                new IntF("gui.corpseorigin.config.f.gecko_interval",
                        () -> gecko.intervalDays, v -> gecko.intervalDays = v,
                        () -> geckoDef.intervalDays, 1, 60, () -> String.valueOf(gecko.intervalDays)),
                new IntF("gui.corpseorigin.config.f.gecko_min_radius",
                        () -> gecko.minRadius, v -> gecko.minRadius = v,
                        () -> geckoDef.minRadius, 8, 96, () -> String.valueOf(gecko.minRadius)),
                new IntF("gui.corpseorigin.config.f.gecko_max_radius",
                        () -> gecko.maxRadius, v -> gecko.maxRadius = v,
                        () -> geckoDef.maxRadius, 16, 128, () -> String.valueOf(gecko.maxRadius)),
                new IntF("gui.corpseorigin.config.f.gecko_nearby_check",
                        () -> gecko.nearbyBossCheck, v -> gecko.nearbyBossCheck = v,
                        () -> geckoDef.nearbyBossCheck, 16, 256, () -> String.valueOf(gecko.nearbyBossCheck)));
        pages.add(new Page("gui.corpseorigin.config.page_gecko", geckoFields));

        // ---- 第 1f 页：漫展尸兄降临（本地皮肤尸群事件）----
        var manzhan = spawn.manzhan;
        var manzhanDef = new CorpseConfig.Spawn.Manzhan();
        List<Field> manzhanFields = List.of(
                new BoolF("gui.corpseorigin.config.f.manzhan_enabled",
                        () -> manzhan.enabled, v -> manzhan.enabled = v,
                        () -> manzhanDef.enabled),
                new IntF("gui.corpseorigin.config.f.manzhan_first_day",
                        () -> manzhan.firstDay, v -> manzhan.firstDay = v,
                        () -> manzhanDef.firstDay, 0, 100,
                        () -> manzhan.firstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(manzhan.firstDay)),
                new IntF("gui.corpseorigin.config.f.manzhan_interval",
                        () -> manzhan.intervalDays, v -> manzhan.intervalDays = v,
                        () -> manzhanDef.intervalDays, 1, 60, () -> String.valueOf(manzhan.intervalDays)),
                new IntF("gui.corpseorigin.config.f.manzhan_count",
                        () -> manzhan.count, v -> manzhan.count = v,
                        () -> manzhanDef.count, 1, 64, () -> String.valueOf(manzhan.count)),
                new IntF("gui.corpseorigin.config.f.manzhan_min_radius",
                        () -> manzhan.minRadius, v -> manzhan.minRadius = v,
                        () -> manzhanDef.minRadius, 8, 96, () -> String.valueOf(manzhan.minRadius)),
                new IntF("gui.corpseorigin.config.f.manzhan_max_radius",
                        () -> manzhan.maxRadius, v -> manzhan.maxRadius = v,
                        () -> manzhanDef.maxRadius, 16, 128, () -> String.valueOf(manzhan.maxRadius)),
                new IntF("gui.corpseorigin.config.f.manzhan_nearby_check",
                        () -> manzhan.nearbyCheck, v -> manzhan.nearbyCheck = v,
                        () -> manzhanDef.nearbyCheck, 16, 256, () -> String.valueOf(manzhan.nearbyCheck)),
                new ActionF("gui.corpseorigin.config.f.manzhan_open_folder",
                        () -> "gui.corpseorigin.config.f.manzhan_open_folder_btn",
                        () -> {
                            try {
                                // 不存在就建目录（文件名=皮肤名的 PNG 放这里），再用系统文件管理器打开
                                java.nio.file.Path dir = xiaoshi2022.corpseorigin.skin.LocalSkinNames.folder();
                                java.nio.file.Files.createDirectories(dir);
                                net.minecraft.util.Util.getPlatform().openPath(dir.toAbsolutePath());
                            } catch (Exception e) {
                                xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("打开本地皮肤文件夹失败", e);
                            }
                        }));
        pages.add(new Page("gui.corpseorigin.config.page_manzhan", manzhanFields));

        // ---- 第 1g 页：尸兄玩家皮肤池（/summonzb 随机皮肤名单，实时增删）----
        var names = c.names;
        List<Field> skinPoolFields = List.of(
                new InfoF("gui.corpseorigin.config.f.skinpool_current", () -> {
                    var n = names;
                    int total = n.consentedIds.size() + n.ids.size();
                    if (total == 0) {
                        return net.minecraft.client.resources.language.I18n.get(
                                "gui.corpseorigin.config.f.skinpool_empty");
                    }
                    var show = new ArrayList<String>(n.consentedIds);
                    show.addAll(n.ids);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.min(4, show.size()); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(show.get(i));
                    }
                    if (show.size() > 4) sb.append("…");
                    return total + ": " + sb;
                }),
                new TextF("gui.corpseorigin.config.f.skinpool_add",
                        () -> "gui.corpseorigin.config.f.skinpool_add_hint",
                        v -> {
                            // 合法 MC 用户名才收（3~16 位英文/数字/下划线），去重
                            if (v.matches("[A-Za-z0-9_]{3,16}") && !names.ids.contains(v)) {
                                var list = new ArrayList<>(names.ids);
                                list.add(v);
                                names.ids = List.copyOf(list);
                            }
                            net.minecraft.client.Minecraft.getInstance().execute(this::rebuildWidgets);
                        }),
                new TextF("gui.corpseorigin.config.f.skinpool_remove",
                        () -> "gui.corpseorigin.config.f.skinpool_remove_hint",
                        v -> {
                            if (names.ids.contains(v)) {
                                var list = new ArrayList<>(names.ids);
                                list.remove(v);
                                names.ids = List.copyOf(list);
                            }
                            if (names.consentedIds.contains(v)) {
                                var list2 = new ArrayList<>(names.consentedIds);
                                list2.remove(v);
                                names.consentedIds = List.copyOf(list2);
                            }
                            net.minecraft.client.Minecraft.getInstance().execute(this::rebuildWidgets);
                        }));
        pages.add(new Page("gui.corpseorigin.config.page_skinpool", skinPoolFields));

        // ---- 第 7 页：世界威胁等级（尸兄强度随玩家最高境界缩放，见 WorldThreatManager）----
        var threat = spawn.worldThreat;
        var threatDef = new CorpseConfig.Spawn.WorldThreat();
        List<Field> threatFields = List.of(
                new BoolF("gui.corpseorigin.config.f.threat_enabled",
                        () -> threat.enabled, v -> threat.enabled = v,
                        () -> threatDef.enabled),
                new FloatF("gui.corpseorigin.config.f.threat_hp",
                        () -> threat.hpPerLevel, v -> threat.hpPerLevel = (float) v,
                        () -> threatDef.hpPerLevel, 0F, 0.5F,
                        () -> String.format("+%.0f%%/级", threat.hpPerLevel * 100)),
                new FloatF("gui.corpseorigin.config.f.threat_damage",
                        () -> threat.damagePerLevel, v -> threat.damagePerLevel = (float) v,
                        () -> threatDef.damagePerLevel, 0F, 0.5F,
                        () -> String.format("+%.0f%%/级", threat.damagePerLevel * 100)),
                new FloatF("gui.corpseorigin.config.f.threat_armor",
                        () -> threat.armorPerLevel, v -> threat.armorPerLevel = (float) v,
                        () -> threatDef.armorPerLevel, 0F, 3F,
                        () -> String.format("+%.2f/级", threat.armorPerLevel)),
                new IntF("gui.corpseorigin.config.f.threat_max_levels",
                        () -> threat.maxLevelsCounted, v -> threat.maxLevelsCounted = v,
                        () -> threatDef.maxLevelsCounted, 1, 20, () -> String.valueOf(threat.maxLevelsCounted)));
        pages.add(new Page("gui.corpseorigin.config.page_threat", threatFields));

        // ---- 第 8 页：尸兄饥饿（衰减速度/个体随机/阈值，见 LowerLevelZbEntity）----
        var zbHunger = spawn.zbHunger;
        var zbHungerDef = new CorpseConfig.Spawn.ZbHunger();
        List<Field> zbHungerFields = List.of(
                new IntF("gui.corpseorigin.config.f.zb_hunger_seconds",
                        () -> zbHunger.secondsPerPoint, v -> zbHunger.secondsPerPoint = v,
                        () -> zbHungerDef.secondsPerPoint, 0, 600,
                        () -> zbHunger.secondsPerPoint == 0 ? "关闭" : zbHunger.secondsPerPoint + "秒/点"),
                new IntF("gui.corpseorigin.config.f.zb_hunger_random",
                        () -> zbHunger.randomPercent, v -> zbHunger.randomPercent = v,
                        () -> zbHungerDef.randomPercent, 0, 90,
                        () -> "±" + zbHunger.randomPercent + "%"),
                new IntF("gui.corpseorigin.config.f.zb_hunger_threshold",
                        () -> zbHunger.hungerThreshold, v -> zbHunger.hungerThreshold = v,
                        () -> zbHungerDef.hungerThreshold, 1, 100,
                        () -> "低于" + zbHunger.hungerThreshold + "算饿"),
                new IntF("gui.corpseorigin.config.f.zb_hunger_feeding_level",
                        () -> zbHunger.feedingMinLevel, v -> zbHunger.feedingMinLevel = v,
                        () -> zbHungerDef.feedingMinLevel, 0, 20,
                        () -> "≥" + zbHunger.feedingMinLevel + "级才啃尸"));
        pages.add(new Page("gui.corpseorigin.config.page_zb_hunger", zbHungerFields));

        // ---- 第 2 页：尸兄虫事件 + 尸兄进化 ----
        List<Field> wormFields = List.of(
                new IntF("gui.corpseorigin.config.f.worm_first_day",
                        () -> spawn.corpseWormFirstDay, v -> spawn.corpseWormFirstDay = v,
                        () -> spawnDef.corpseWormFirstDay, 0, 100,
                        () -> spawn.corpseWormFirstDay == 0
                                ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.config.disabled")
                                : String.valueOf(spawn.corpseWormFirstDay)),
                new IntF("gui.corpseorigin.config.f.worm_interval",
                        () -> spawn.corpseWormBaseIntervalDays, v -> spawn.corpseWormBaseIntervalDays = v,
                        () -> spawnDef.corpseWormBaseIntervalDays, 1, 30,
                        () -> String.valueOf(spawn.corpseWormBaseIntervalDays)),
                new IntF("gui.corpseorigin.config.f.worm_interval_increase",
                        () -> spawn.corpseWormIntervalIncreaseDays, v -> spawn.corpseWormIntervalIncreaseDays = v,
                        () -> spawnDef.corpseWormIntervalIncreaseDays, 0, 10,
                        () -> String.valueOf(spawn.corpseWormIntervalIncreaseDays)),
                // ---- 野生尸兄：按游戏日掷初始进化等级（rollSpawnLevel）----
                new BoolF("gui.corpseorigin.config.f.zb_evo_ramp",
                        () -> spawn.evolutionLevelRamp, v -> spawn.evolutionLevelRamp = v,
                        () -> spawnDef.evolutionLevelRamp),
                new IntF("gui.corpseorigin.config.f.zb_evo_days_per_level",
                        () -> spawn.daysPerEvolutionLevel, v -> spawn.daysPerEvolutionLevel = v,
                        () -> spawnDef.daysPerEvolutionLevel, 1, 64,
                        () -> String.valueOf(spawn.daysPerEvolutionLevel)),
                new IntF("gui.corpseorigin.config.f.zb_evo_max_level",
                        () -> spawn.maxEvolutionLevel, v -> spawn.maxEvolutionLevel = v,
                        () -> spawnDef.maxEvolutionLevel, 1, 10,
                        () -> String.valueOf(spawn.maxEvolutionLevel)),
                new FloatF("gui.corpseorigin.config.f.zb_evo_decay",
                        () -> spawn.levelDecay, v -> spawn.levelDecay = (float) v,
                        () -> (double) spawnDef.levelDecay, 1.1F, 6F, () -> fmtFloat(spawn.levelDecay)),
                // ---- 吸食进化：尸兄吃血肉自己升级（ZbEvolution）----
                new IntF("gui.corpseorigin.config.f.zb_evo_energy",
                        () -> spawn.zbEvolutionEnergyPerLevel, v -> spawn.zbEvolutionEnergyPerLevel = v,
                        () -> spawnDef.zbEvolutionEnergyPerLevel, 1, 100,
                        () -> String.valueOf(spawn.zbEvolutionEnergyPerLevel)),
                new IntF("gui.corpseorigin.config.f.zb_evo_breakthrough_level",
                        () -> spawn.zbEvolutionBreakthroughLevel, v -> spawn.zbEvolutionBreakthroughLevel = v,
                        () -> spawnDef.zbEvolutionBreakthroughLevel, 1, 9,
                        () -> String.valueOf(spawn.zbEvolutionBreakthroughLevel)),
                new FloatF("gui.corpseorigin.config.f.zb_evo_breakthrough_chance",
                        () -> spawn.zbEvolutionBreakthroughChance, v -> spawn.zbEvolutionBreakthroughChance = (float) v,
                        () -> (double) spawnDef.zbEvolutionBreakthroughChance, 0F, 1F,
                        () -> fmtPct(spawn.zbEvolutionBreakthroughChance)),
                new FloatF("gui.corpseorigin.config.f.zb_evo_breakthrough_bonus",
                        () -> spawn.zbEvolutionBreakthroughBonus, v -> spawn.zbEvolutionBreakthroughBonus = (float) v,
                        () -> (double) spawnDef.zbEvolutionBreakthroughBonus, 0F, 0.5F,
                        () -> fmtPct(spawn.zbEvolutionBreakthroughBonus)),
                // ---- 感染系统：被咬/寄生后的尸化概率（CorpseHorrorConfig）----
                new FloatF("gui.corpseorigin.config.f.infect_player_bite",
                        () -> horror.playerBiteInfectionChance, v -> horror.playerBiteInfectionChance = v,
                        () -> horrorDef.playerBiteInfectionChance, 0F, 1F,
                        () -> fmtPct(horror.playerBiteInfectionChance)),
                new FloatF("gui.corpseorigin.config.f.infect_villager_bite",
                        () -> horror.villagerInfectionChance, v -> horror.villagerInfectionChance = v,
                        () -> horrorDef.villagerInfectionChance, 0F, 1F,
                        () -> fmtPct(horror.villagerInfectionChance)),
                new FloatF("gui.corpseorigin.config.f.infect_fish_water",
                        () -> horror.fishWaterInfectionChance, v -> horror.fishWaterInfectionChance = v,
                        () -> horrorDef.fishWaterInfectionChance, 0F, 1F,
                        () -> fmtPct(horror.fishWaterInfectionChance)),
                new IntF("gui.corpseorigin.config.f.infect_maggot_ticks",
                        () -> horror.maggotInfectionTicks, v -> horror.maggotInfectionTicks = v,
                        () -> horrorDef.maggotInfectionTicks, 100, 1200,
                        () -> String.valueOf(horror.maggotInfectionTicks)));
        pages.add(new Page("gui.corpseorigin.config.page_worm", wormFields));

        // ---- 第 3 页：尸兄生成权重 ----
        List<Field> spawnFields = new ArrayList<>();
        spawnFields.add(new IntF("gui.corpseorigin.config.f.lower_level_zb_weight",
                () -> spawn.lowerLevelZbWeight, v -> spawn.lowerLevelZbWeight = v,
                () -> spawnDef.lowerLevelZbWeight, 0, 200, () -> String.valueOf(spawn.lowerLevelZbWeight)));
        spawnFields.add(new IntF("gui.corpseorigin.config.f.aotuman_zb_weight",
                () -> spawn.aotumanZbWeight, v -> spawn.aotumanZbWeight = v,
                () -> spawnDef.aotumanZbWeight, 0, 200, () -> String.valueOf(spawn.aotumanZbWeight)));
        spawnFields.add(new IntF("gui.corpseorigin.config.f.miku_zb_weight",
                () -> spawn.mikuZbWeight, v -> spawn.mikuZbWeight = v,
                () -> spawnDef.mikuZbWeight, 0, 200, () -> String.valueOf(spawn.mikuZbWeight)));
        spawnFields.add(new IntF("gui.corpseorigin.config.f.coco_zombie_weight",
                () -> spawn.cocoZombieWeight, v -> spawn.cocoZombieWeight = v,
                () -> spawnDef.cocoZombieWeight, 0, 200, () -> String.valueOf(spawn.cocoZombieWeight)));
        spawnFields.add(new IntF("gui.corpseorigin.config.f.ham_weight",
                () -> spawn.hamWeight, v -> spawn.hamWeight = v,
                () -> spawnDef.hamWeight, 0, 40, () -> String.valueOf(spawn.hamWeight)));
        spawnFields.add(new BoolF("gui.corpseorigin.config.f.disable_vanilla_zombie",
                () -> spawn.disableVanillaZombieSpawns, v -> spawn.disableVanillaZombieSpawns = v,
                () -> spawnDef.disableVanillaZombieSpawns));
        pages.add(new Page("gui.corpseorigin.config.page_spawn", spawnFields));

        // ---- 第 4 页：战斗表现 ----
        pages.add(new Page("gui.corpseorigin.config.page_combat", List.of(
                new BoolF("gui.corpseorigin.config.f.hit_stop",
                        () -> sv.hitStop, v -> sv.hitStop = v, () -> svDef.hitStop),
                new BoolF("gui.corpseorigin.config.f.screen_effects",
                        () -> sv.screenEffects, v -> sv.screenEffects = v, () -> svDef.screenEffects),
                new FloatF("gui.corpseorigin.config.f.camera_shake",
                        () -> sv.cameraShake, v -> sv.cameraShake = (float) v,
                        () -> (double) svDef.cameraShake, 0F, 2F, () -> fmtFloat(sv.cameraShake)),
                new FloatF("gui.corpseorigin.config.f.flash_intensity",
                        () -> sv.flashIntensity, v -> sv.flashIntensity = (float) v,
                        () -> (double) svDef.flashIntensity, 0F, 1F, () -> fmtFloat(sv.flashIntensity)),
                new IntF("gui.corpseorigin.config.f.max_impact_effects",
                        () -> sv.maxImpactEffects, v -> sv.maxImpactEffects = v,
                        () -> svDef.maxImpactEffects, 0, 128, () -> String.valueOf(sv.maxImpactEffects)),
                // 吸血鬼 K 的阳光防晒总闸（原 /vampire_sunlight 命令的同一开关，实时读 growth 节）
                new BoolF("gui.corpseorigin.config.f.vampire_sunlight_damage",
                        () -> growth.vampireSunlightDamage, v -> growth.vampireSunlightDamage = v,
                        () -> growthDef.vampireSunlightDamage))));

        // ---- 第 5 页：葫芦继承 ----
        pages.add(new Page("gui.corpseorigin.config.page_gourd", List.of(
                new BoolF("gui.corpseorigin.config.f.gourd_enabled",
                        () -> gi.enabled, v -> gi.enabled = v, () -> giDef.enabled),
                new FloatF("gui.corpseorigin.config.f.gourd_passive",
                        () -> gi.passiveChance, v -> gi.passiveChance = (float) v,
                        () -> giDef.passiveChance, 0F, 1F, () -> fmtPct(gi.passiveChance)),
                new FloatF("gui.corpseorigin.config.f.gourd_neutral",
                        () -> gi.neutralChance, v -> gi.neutralChance = (float) v,
                        () -> giDef.neutralChance, 0F, 1F, () -> fmtPct(gi.neutralChance)),
                new FloatF("gui.corpseorigin.config.f.gourd_hostile",
                        () -> gi.hostileChance, v -> gi.hostileChance = (float) v,
                        () -> giDef.hostileChance, 0F, 1F, () -> fmtPct(gi.hostileChance)),
                new FloatF("gui.corpseorigin.config.f.gourd_elite",
                        () -> gi.eliteChance, v -> gi.eliteChance = (float) v,
                        () -> giDef.eliteChance, 0F, 1F, () -> fmtPct(gi.eliteChance)),
                new FloatF("gui.corpseorigin.config.f.gourd_boss",
                        () -> gi.bossChance, v -> gi.bossChance = (float) v,
                        () -> giDef.bossChance, 0F, 1F, () -> fmtPct(gi.bossChance)),
                new FloatF("gui.corpseorigin.config.f.gourd_auto_mult",
                        () -> gi.automaticMultiplier, v -> gi.automaticMultiplier = (float) v,
                        () -> giDef.automaticMultiplier, 0F, 1F, () -> fmtPct(gi.automaticMultiplier)))));

        // ---- 第 6 页：剑气与崩星威压（地形破坏防毁图开关）----
        pages.add(new Page("gui.corpseorigin.config.page_burst", List.of(
                // 神级剑气开槽的总闸（SwordRift 发动与执行都检查）
                new BoolF("gui.corpseorigin.config.f.sword_terrain_destruction",
                        () -> realm.swordTerrainDestruction, v -> realm.swordTerrainDestruction = v,
                        () -> realmDef.swordTerrainDestruction),
                // 同时劈山：同一时间允许存在的剑气道数上限
                new IntF("gui.corpseorigin.config.f.sword_rift_concurrent",
                        () -> realm.swordRiftConcurrent, v -> realm.swordRiftConcurrent = v,
                        () -> realmDef.swordRiftConcurrent, 1, 8, () -> String.valueOf(realm.swordRiftConcurrent)),
                // 崩星威压（神上 EX）的地形破坏
                new BoolF("gui.corpseorigin.config.f.burst_breaks_terrain",
                        () -> realm.burstBreaksTerrain, v -> realm.burstBreaksTerrain = v,
                        () -> realmDef.burstBreaksTerrain),
                new IntF("gui.corpseorigin.config.f.burst_qi_cost",
                        () -> realm.burstQiCost, v -> realm.burstQiCost = v,
                        () -> realmDef.burstQiCost, 100, 100000, () -> String.valueOf(realm.burstQiCost)),
                new IntF("gui.corpseorigin.config.f.burst_cooldown",
                        () -> realm.burstCooldownTicks, v -> realm.burstCooldownTicks = v,
                        () -> realmDef.burstCooldownTicks, 20, 72000, () -> String.valueOf(realm.burstCooldownTicks)),
                new FloatF("gui.corpseorigin.config.f.burst_radius",
                        () -> realm.burstRadius, v -> realm.burstRadius = v,
                        () -> realmDef.burstRadius, 8F, 128F, () -> fmtFloat(realm.burstRadius)))));

        // ---- 第 7 页：境界与难度（全局难度/属性倍率实时调整）----
        pages.add(new Page("gui.corpseorigin.config.page_realm", List.of(
                // 境界养成系统总闸（关闭后战斗表现/剑气/崩星一并停用）
                new BoolF("gui.corpseorigin.config.f.realm_enabled",
                        () -> realm.enabled, v -> realm.enabled = v, () -> realmDef.enabled),
                // 敌人强度难度倍率
                new FloatF("gui.corpseorigin.config.f.difficulty_multiplier",
                        () -> realm.difficultyMultiplier, v -> realm.difficultyMultiplier = v,
                        () -> realmDef.difficultyMultiplier, 1F, 40F, () -> fmtFloat(realm.difficultyMultiplier)),
                // 玩家属性成长倍率
                new FloatF("gui.corpseorigin.config.f.stat_multiplier",
                        () -> realm.statMultiplier, v -> realm.statMultiplier = v,
                        () -> realmDef.statMultiplier, .1F, 5F, () -> fmtFloat(realm.statMultiplier)),
                // 内力技能伤害缩放
                new FloatF("gui.corpseorigin.config.f.qi_skill_damage_scaling",
                        () -> realm.qiSkillDamageScaling, v -> realm.qiSkillDamageScaling = v,
                        () -> realmDef.qiSkillDamageScaling, 0F, 5F, () -> fmtFloat(realm.qiSkillDamageScaling)),
                // 冷却缩减：每级 +X%，上限 Y%
                new FloatF("gui.corpseorigin.config.f.cooldown_reduction_per_level",
                        () -> realm.cooldownReductionPerLevel, v -> realm.cooldownReductionPerLevel = v,
                        () -> realmDef.cooldownReductionPerLevel, 0F, .2F, () -> fmtPct(realm.cooldownReductionPerLevel)),
                new FloatF("gui.corpseorigin.config.f.max_cooldown_reduction",
                        () -> realm.maxCooldownReduction, v -> realm.maxCooldownReduction = v,
                        () -> realmDef.maxCooldownReduction, 0F, .95F, () -> fmtPct(realm.maxCooldownReduction)),
                // 充能（点数换血/内力/血库）价格
                new IntF("gui.corpseorigin.config.f.recharge_point_cost",
                        () -> realm.rechargePointCost, v -> realm.rechargePointCost = v,
                        () -> realmDef.rechargePointCost, 1, 10000, () -> String.valueOf(realm.rechargePointCost)),
                // 身法速度曲线：地面加值上限 + 飞行灵敏度
                new FloatF("gui.corpseorigin.config.f.speed_bonus_cap",
                        () -> realm.speedBonusCap, v -> realm.speedBonusCap = v,
                        () -> realmDef.speedBonusCap, .05F, 1F, () -> fmtFloat(realm.speedBonusCap)),
                new FloatF("gui.corpseorigin.config.f.flight_sensitivity",
                        () -> realm.flightSensitivity, v -> realm.flightSensitivity = v,
                        () -> realmDef.flightSensitivity, 1F, 20F, () -> fmtFloat(realm.flightSensitivity)))));

        pageIndex = Math.min(pageIndex, pages.size() - 1);
    }

    // ==================== init ====================

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        labels.clear();
        scrollEntries.clear();
        scrollOffset = 0;

        if (edited == null) edited = CorpseConfig.snapshot();
        buildPages();

        int screenW = this.width > 0 ? this.width
                : net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = this.height > 0 ? this.height
                : net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        cachedScreenW = screenW;
        cachedScreenH = screenH;

        int panelW = Math.max(260, Math.min(PANEL_W_MAX, screenW - 40));
        cachedPanelW = panelW;
        int cx = screenW / 2;
        int panelLeft = cx - panelW / 2;
        int labelX = panelLeft + PAD;
        int sliderX = labelX + LABEL_W;
        cachedPanelLeft = panelLeft;
        cachedTitleCx = cx;
        cachedPanelTop = TOP_Y - 8;

        viewTop = TOP_Y + 22;
        viewBottom = Math.max(viewTop + 40, screenH - BOTTOM_BAR_H - 8);

        int row = viewTop + 4;
        Page page = pages.get(pageIndex);
        addLabel(labelX, row, page.titleKey(), 0xFF55FFFF);
        row += ROW_H + 2;

        for (Field f : page.fields()) {
            addLabel(labelX, row, f.labelKey, 0xFFFFFFFF);
            AbstractWidget w = f.widget(sliderX, row);
            addRenderableWidget(w);
            scrollEntries.add(new ScrollEntry(w, sliderX, row));
            row += ROW_H + ROW_GAP;
        }

        contentHeight = row - viewTop;
        applyScroll();

        // ---- 顶部翻页 ----
        int pageBtnY = TOP_Y - 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                    pageIndex = (pageIndex - 1 + pages.size()) % pages.size();
                    rebuildWidgets();
                }).bounds(panelLeft + PAD, pageBtnY, PAGE_BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                    pageIndex = (pageIndex + 1) % pages.size();
                    rebuildWidgets();
                }).bounds(panelLeft + panelW - PAD - PAGE_BTN_W, pageBtnY, PAGE_BTN_W, BTN_H).build());

        // ---- 底部按钮 ----
        int bottomRow = screenH - BOTTOM_BAR_H + 6;
        int gap = 6;
        // 三颗主按钮：重置本页 | HUD设置 | 保存并关闭
        int totalW = BTN_W * 3 + gap * 2;
        int startX = panelLeft + (panelW - totalW) / 2;
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.config.reset_page"),
                        b -> { pages.get(pageIndex).fields().forEach(f -> f.reset.run()); rebuildWidgets(); })
                .bounds(startX, bottomRow, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.config.hud_settings"),
                        b -> net.minecraft.client.Minecraft.getInstance()
                                .gui.setScreen(new HudSettingsScreen()))
                .bounds(startX + BTN_W + gap, bottomRow, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.config.save_close"),
                        b -> { CorpseConfig.replace(edited); edited = null; onClose(); })
                .bounds(startX + (BTN_W + gap) * 2, bottomRow, BTN_W, BTN_H).build());
        // 取消按钮单独一行居中
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.corpseorigin.config.cancel"),
                        b -> { edited = null; onClose(); })
                .bounds(panelLeft + (panelW - BTN_W) / 2, bottomRow + BTN_H + 2, BTN_W, BTN_H).build());

        cachedPanelBottom = screenH - 4;
    }

    // ==================== 滚动 ====================

    private int maxScroll() {
        return Math.max(0, contentHeight - (viewBottom - viewTop) + 12);
    }

    private void applyScroll() {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        for (ScrollEntry e : scrollEntries) {
            int drawY = e.baseY() - scrollOffset;
            e.widget().setY(drawY);
            boolean visible = drawY + e.widget().getHeight() > viewTop && drawY < viewBottom;
            e.widget().visible = visible;
            e.widget().active = visible;
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

    // ==================== 工厂 ====================

    private void addLabel(int x, int y, String key, int color) {
        labels.add(new Label(x, y, key, color));
    }

    private static String fmtFloat(double v) {
        return String.format("%.2f", v);
    }

    private static String fmtPct(double v) {
        return String.format("%.0f%%", v * 100);
    }

    // ==================== 渲染 ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, cachedScreenW, cachedScreenH, 0x66000000);
        graphics.fill(cachedPanelLeft - 2, cachedPanelTop,
                cachedPanelLeft + cachedPanelW + 2, cachedPanelBottom, 0xCC111111);

        // 标题 + 页码
        graphics.centeredText(font, getTitle(), cachedTitleCx, TOP_Y - 20, 0xFFFFFFFF);
        String pageInfo = net.minecraft.client.resources.language.I18n.get(
                "gui.corpseorigin.config.page_of", pageIndex + 1, pages.size())
                + " · " + net.minecraft.client.resources.language.I18n.get(pages.get(pageIndex).titleKey());
        graphics.centeredText(font, Component.literal(pageInfo), cachedTitleCx, TOP_Y, 0xFFAAAAAA);

        // 字段标签（跟随滚动）
        for (Label l : labels) {
            int drawY = l.baseY() - scrollOffset;
            if (drawY + 12 < viewTop || drawY > viewBottom) continue;
            graphics.text(font, Component.translatable(l.key()), l.baseX(), drawY + 6, l.color(), false);
        }

        // 滚动条
        int max = maxScroll();
        if (max > 0) {
            int trackX = cachedPanelLeft + cachedPanelW - 6;
            int trackH = viewBottom - viewTop;
            int barH = Math.max(20, trackH * trackH / contentHeight);
            int barY = viewTop + (trackH - barH) * scrollOffset / max;
            graphics.fill(trackX, viewTop, trackX + 4, viewBottom, 0x44FFFFFF);
            graphics.fill(trackX, barY, trackX + 4, barY + barH, 0xFFAAAAAA);
        }

        // 底部提示：服务端参数需 reload
        graphics.centeredText(font,
                Component.translatable("gui.corpseorigin.config.reload_hint"),
                cachedTitleCx, cachedScreenH - 10, 0xFF888888);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
