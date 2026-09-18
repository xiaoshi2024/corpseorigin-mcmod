package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色选择书 GUI —— 列出全部已注册角色，点一位就换过去，然后这本书消失。
 * <p>
 * 界面本身<b>不做任何判定</b>：点中的只是把角色 ID 发给服务端（{@code CharacterBookSelectC2S}），
 * "手里是否真的拿着书 / 是否要扣书 / 换人是否成功"全部在服务端算（见
 * {@code CharacterBookItem#selectFromBook}），所以这里没有可以被客户端绕过的权限点。
 * <p>
 * 列表与技能树同款：一屏放不下就用<b>滚轮</b>滑动（右侧有滚动条），面板宽度随窗口收窄。
 * 当前角色会标成绿色并且不能被再次点选（避免白扣一本）。
 */
@Environment(EnvType.CLIENT)
public class CharacterBookScreen extends Screen {

    /** 每行高度：一行名字 + 一行描述 */
    private static final int ROW_HEIGHT = 34;
    /** 顶部标题 + 副标题占用掉的固定高度 */
    private static final int HEADER = 48;
    /** 底部滚动提示占用掉的高度 */
    private static final int FOOTER = 20;
    /** 面板最大宽度（窗口更窄时自动收窄） */
    private static final int PANEL_MAX_WIDTH = 300;

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
        // 客户端在 onInitialize 里跑过 CharacterManager#registerDefaults，所以这份列表是完整的
        characters = CharacterManager.getInstance().getRegisteredCharacters();
        currentId = CharacterManager.getInstance().getClientCachedCharacterId();
        scrollRow = Mth.clamp(scrollRow, 0, maxScrollRow());
    }

    // ==================== 布局 ====================

    private int panelWidth() {
        return Math.max(140, Math.min(PANEL_MAX_WIDTH, width - 40));
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

        int x = panelX();
        int w = panelWidth();

        for (int i = 0; i < characters.size(); i++) {
            int y = rowY(i);
            if (y < 0) {
                continue;
            }
            ICharacter character = characters.get(i);
            boolean current = isCurrent(character);
            boolean hoveredRow = isHovered(i, mouseX, mouseY);
            boolean clickable = hoveredRow && !current;

            int bg = current ? 0x88204420 : clickable ? 0x88303030 : 0x88202020;
            graphics.fill(x, y, x + w, y + ROW_HEIGHT - 3, bg);
            graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 3, current ? 0xFF55FF55 : 0xFFFFAA00);

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
