package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xiaoshi2022.corpseorigin.character.CharacterFaction;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色选择书 GUI —— 顶部 5 个阵营 Tab，点击切换阵营；下方列出该阵营内全部角色，
 * 点一位就换过去，然后这本书消失。
 * <p>
 * 界面本身<b>不做任何判定</b>：点中的只是把角色 ID 发给服务端（{@code CharacterBookSelectC2S}），
 * "手里是否真的拿着书 / 是否要扣书 / 换人是否成功"全部在服务端算（见
 * {@code CharacterBookItem#selectFromBook}），所以这里没有可以被客户端绕过的权限点。
 * <p>
 * 当前角色会标成绿色并且不能被再次点选（避免白扣一本）。
 */
@Environment(EnvType.CLIENT)
public class CharacterBookScreen extends Screen {

    /** 每行高度：一行名字 + 一行描述 */
    private static final int ROW_HEIGHT = 34;
    /** 顶部标题区占用的高度（标题 + 副标题 + Tab 按钮） */
    private static final int HEADER = 80;
    /** 底部滚动提示占用掉的高度 */
    private static final int FOOTER = 20;
    /** 面板最大宽度（窗口更窄时自动收窄） */
    private static final int PANEL_MAX_WIDTH = 360;

    /** 所有阵营（按枚举顺序） */
    private CharacterFaction[] factions;
    /** 当前选中的阵营下标 */
    private int factionIndex;
    /** 当前阵营下要展示的角色列表（每切 Tab 重算） */
    private List<ICharacter> characters = List.of();

    /** 当前屏幕顶部那条可见的行下标（滚轮滑动用） */
    private int scrollRow;
    /** 当前角色 ID —— 列表里标出来，也不允许原地重选 */
    private String currentId;

    public CharacterBookScreen() {
        super(Component.translatable("gui.corpseorigin.character_book"));
    }

    /** 客户端右键「统一角色书」时打开（见 {@code CharacterBookItem#use}） */
    public static void open() {
        Minecraft.getInstance().gui.setScreen(new CharacterBookScreen());
    }

    @Override
    protected void init() {
        CharacterManager manager = CharacterManager.getInstance();
        factions = manager.getAllFactions();
        currentId = manager.getClientCachedCharacterId();

        // 切 Tab 时重置 scrollRow，避免残留到新阵营里
        scrollRow = 0;
        characters = manager.getCharactersByFaction(factions[factionIndex]);

        // ===== 顶部阵营 Tab 按钮 =====
        int tabGap = 4;
        // 让 Tab 栏总宽度 = 面板宽度
        int totalTabWidth = panelWidth();
        int tabWidth = Math.max(40, (totalTabWidth - tabGap * (factions.length - 1)) / factions.length);
        int tabX0 = panelX();
        int tabY = 44;

        for (int i = 0; i < factions.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(
                    Component.translatable(factions[i].getTranslationKey()),
                    b -> switchFaction(idx))
                    .bounds(tabX0 + i * (tabWidth + tabGap), tabY, tabWidth, 24)
                    .build());
        }
    }

    /** 切换阵营 Tab —— 只更新数据，Button 保持不变 */
    private void switchFaction(int newIndex) {
        factionIndex = Mth.clamp(newIndex, 0, factions.length - 1);
        scrollRow = 0;
        characters = CharacterManager.getInstance().getCharactersByFaction(factions[factionIndex]);
    }

    // ==================== 布局 ====================

    private int panelWidth() {
        return Math.max(200, Math.min(PANEL_MAX_WIDTH, width - 40));
    }

    private int panelX() {
        return width / 2 - panelWidth() / 2;
    }

    /** 一屏最多显示几行 */
    private int visibleRows() {
        return Math.max(1, (height - HEADER - FOOTER) / ROW_HEIGHT);
    }

    /** 最多能往上滑几行（0 = 一屏放得下，不需要滑动） */
    private int maxScrollRow() {
        return Math.max(0, characters.size() - visibleRows());
    }

    /** 行下标 → 屏幕 y；这一行当前不可见时返回 -1（直接跳过绘制/点击） */
    private int rowY(int index) {
        int visibleIndex = index - scrollRow;
        if (visibleIndex < 0 || visibleIndex >= visibleRows()) {
            return -1;
        }
        return HEADER + visibleIndex * ROW_HEIGHT;
    }

    /** 这一行是不是当前角色 */
    private boolean isCurrent(ICharacter character) {
        return character.getId().equals(currentId);
    }

    /** 鼠标是不是落在这条上（当前角色那行不接受点击，所以 hovered 也算它不算） */
    private boolean isHovered(int index, int mouseX, int mouseY) {
        int y = rowY(index);
        if (y < 0) {
            return false;
        }
        int x = panelX();
        return mouseX >= x && mouseX <= x + panelWidth()
                && mouseY >= y && mouseY <= y + ROW_HEIGHT - 3;
    }

    /** 滚轮滑动（只在放不下时才吃事件，否则交还给父类） */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScrollRow() > 0 && scrollY != 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ==================== 绘制 ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // 挑角色要盯着看一会儿，压暗背景好读
        graphics.fill(0, 0, width, height, 0xB0000000);

        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("gui.corpseorigin.character_book.hint"),
                width / 2, 28, 0xFFAAAAAA);

        // 当前阵营名 + 角色数（Tab 上方不显示，因为 Button 自己已经显示阵营名）

        int x = panelX();
        int w = panelWidth();

        // ========== 阵营 Tab 高亮 ==========
        // Button.builder 本身不自动高亮当前选中 Tab —— 手动给当前选中 Tab 画一层半透明背景 + 阵营色底线
        CharacterFaction currentFaction = factions[factionIndex];
        int tabGap = 4;
        int tabWidth = Math.max(40, (w - tabGap * (factions.length - 1)) / factions.length);
        for (int i = 0; i < factions.length; i++) {
            int tabLeft = x + i * (tabWidth + tabGap);
            int tabTop = 44;
            int tabBottom = tabTop + 24;
            if (i == factionIndex) {
                // 当前 Tab：半透明阵营色背景 + 实色底线
                graphics.fill(tabLeft, tabTop, tabLeft + tabWidth, tabBottom, currentFaction.getColor() & 0x55FFFFFF);
                graphics.fill(tabLeft, tabBottom, tabLeft + tabWidth, tabBottom + 2, currentFaction.getColor());
            }
        }

        // ========== 角色列表 ==========
        if (characters.isEmpty()) {
            // 这个阵营还没有注册角色 —— 提示一下
            graphics.centeredText(font,
                    Component.translatable("gui.corpseorigin.character_book.empty_faction"),
                    width / 2, HEADER + 20, 0xFF888888);
            return;
        }

        for (int i = 0; i < characters.size(); i++) {
            int y = rowY(i);
            if (y < 0) {
                continue;
            }
            ICharacter character = characters.get(i);
            boolean current = isCurrent(character);
            boolean hoveredRow = isHovered(i, mouseX, mouseY);
            boolean clickable = hoveredRow && !current;

            // 左边画阵营色竖条，视觉上把每个角色跟阵营关联起来
            int factionColor = CharacterManager.getInstance().getFaction(character.getId()).getColor();
            int bg = current ? 0x88204420 : clickable ? 0x88303030 : 0x88202020;
            graphics.fill(x, y, x + w, y + ROW_HEIGHT - 3, bg);
            graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 3, current ? 0xFF55FF55 : factionColor);

            graphics.text(font, character.getName(), x + 9, y + 4,
                    current ? 0xFF88FF88 : 0xFFFFFF, true);

            // 描述通常比一行长，按剩余宽度截断（右边要留给状态文字）
            String description = character.getDescription().getString();
            graphics.text(font, font.plainSubstrByWidth(description, w - 100),
                    x + 9, y + 17, 0xFFAAAAAA, false);

            String state = Component.translatable(current
                    ? "gui.corpseorigin.character_book.current"
                    : "gui.corpseorigin.character_book.select").getString();
            graphics.text(font, state, x + w - 8 - font.width(state), y + 11,
                    current ? 0xFF66FF66 : 0xFFFFFF99, false);

            if (hoveredRow) {
                graphics.setComponentTooltipForNextFrame(font, describe(character), mouseX, mouseY);
            }
        }

        // 角色没显示完 → 右侧滚动条 + 底部提示
        if (maxScrollRow() > 0) {
            int trackTop = HEADER;
            int trackHeight = visibleRows() * ROW_HEIGHT;
            int barX = x + w + 4;
            int thumbHeight = Math.max(12, trackHeight * visibleRows() / Math.max(1, characters.size()));
            int thumbY = trackTop + (trackHeight - thumbHeight) * scrollRow / Math.max(1, maxScrollRow());

            graphics.fill(barX, trackTop, barX + 2, trackTop + trackHeight, 0x44FFFFFF);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0xCCFFFFFF);
            graphics.centeredText(font,
                    Component.translatable("gui.corpseorigin.character_book.scroll_hint"),
                    width / 2, height - 12, 0xFF999999);
        }
    }

    /** 悬停提示：名称 + 描述 + 特性 */
    private static List<Component> describe(ICharacter character) {
        List<Component> lines = new ArrayList<>();
        lines.add(character.getName().copy().withStyle(ChatFormatting.GOLD));
        lines.add(character.getDescription().copy().withStyle(ChatFormatting.GRAY));
        for (Component trait : character.getTraits()) {
            lines.add(Component.literal("• ").append(trait).withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        // Tab Button 已经通过 addRenderableWidget 自动处理点击
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            for (int i = 0; i < characters.size(); i++) {
                if (!isHovered(i, mouseX, mouseY)) {
                    continue;
                }
                ICharacter character = characters.get(i);
                if (isCurrent(character)) {
                    return true;   // 已经是这个角色：什么都不做，也不扣书
                }
                ClientPlayNetworking.send(
                        new CorpsePayloads.CharacterBookSelectC2S(character.getId()));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        // 单人游戏也不暂停：换人是服务端在 tick 里做的
        return false;
    }
}
