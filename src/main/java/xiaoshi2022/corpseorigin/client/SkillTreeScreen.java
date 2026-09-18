package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

import java.util.List;

/**
 * 技能进化树 GUI - 列出当前角色全部技能，点击学习。
 * <p>
 * 列表会随窗口高度自适应：一屏放不下的技能用<b>滚轮</b>滑动查看（右侧有滚动条），
 * 面板宽度也会随窗口收窄，避免小分辨率下超出画面。
 */
public class SkillTreeScreen extends Screen {

    private List<ISkill> skills = List.of();
    private static final int ROW_HEIGHT = 30;
    /** 顶部标题 + 进化点占用掉的固定高度（这两行一直可见） */
    private static final int HEADER = 42;
    /** 底部滚动提示占用掉的高度 */
    private static final int FOOTER = 22;
    /** 面板最大宽度（窗口更窄时会自动收窄） */
    private static final int PANEL_MAX_WIDTH = 280;

    /** 当前屏幕顶部那条可见的行下标（滚轮滑动用） */
    private int scrollRow;

    public SkillTreeScreen() {
        super(Component.translatable("gui.corpseorigin.skill_tree"));
    }

    @Override
    protected void init() {
        skills = ClientCharacterCache.getCharacterSkills();
        scrollRow = Mth.clamp(scrollRow, 0, maxScrollRow());
    }

    // ==================== 布局 ====================

    private int panelWidth() {
        return Math.max(120, Math.min(PANEL_MAX_WIDTH, width - 40));
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
        return Math.max(0, skills.size() - visibleRows());
    }

    /** 行下标 → 屏幕 y；这一行当前不可见时返回 -1（直接跳过绘制/点击） */
    private int rowY(int index) {
        int visibleIndex = index - scrollRow;
        if (visibleIndex < 0 || visibleIndex >= visibleRows()) {
            return -1;
        }
        return HEADER + visibleIndex * ROW_HEIGHT;
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(font, title, width / 2, 15, 0xFFFFFF);
        graphics.centeredText(font,
                Component.translatable("gui.corpseorigin.skill_tree.points",
                        ClientState.availablePoints, ClientState.earnedPoints),
                width / 2, 28, 0xFF55FF55);

        int x = panelX();
        int w = panelWidth();
        int level = EvolutionManager.getLevel(ClientState.earnedPoints);

        for (int i = 0; i < skills.size(); i++) {
            int y = rowY(i);
            if (y < 0) {
                continue;
            }
            ISkill skill = skills.get(i);
            boolean learned = ClientState.hasLearned(skill.getId().getPath());
            boolean hovered = mouseX >= x && mouseX <= x + w
                    && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2;
            boolean canLearn = !learned
                    && level >= skill.getRequiredLevel()
                    && ClientState.availablePoints >= skill.getCost()
                    && ClientState.learnedSkills.containsAll(
                    skill.getPrerequisites().stream().map(Identifier::getPath).toList());

            int bg = learned ? 0x88004400 : canLearn ? 0x88444422 : 0x88222222;
            if (hovered) {
                bg |= 0x33000000;
            }
            graphics.fill(x, y, x + w, y + ROW_HEIGHT - 2, bg);
            graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 2,
                    0xFF000000 | skill.getSkillType().getColor());

            int textColor = learned ? 0xFF66FF66 : canLearn ? 0xFFFFFF99 : 0xFF999999;
            graphics.text(font, skill.getName(), x + 8, y + 3, textColor, true);
            graphics.text(font,
                    Component.translatable("gui.corpseorigin.skill_tree.row_info",
                            skill.getCost(), skill.getRequiredLevel(),
                            Component.translatable("skilltype.corpseorigin." + skill.getSkillType().getName())),
                    x + 8, y + 15, 0xFFAAAAAA, false);

            String state;
            if (learned) {
                state = Component.translatable("gui.corpseorigin.skill_tree.learned").getString();
            } else if (canLearn) {
                state = Component.translatable("gui.corpseorigin.skill_tree.click_learn").getString();
            } else {
                // 未解锁：优先显示"获取式"解锁条件（拿到某个器官 / 宠物 / 物品即学会），
                // 这样玩家能知道自己该去找什么，而不是只看到一句"未解锁"
                List<SkillUnlockSource> sources = skill.getUnlockSources();
                if (!sources.isEmpty()) {
                    state = Component.translatable("gui.corpseorigin.skill_tree.requires",
                            sources.get(0).describe()).getString();
                } else {
                    state = Component.translatable("gui.corpseorigin.skill_tree.locked").getString();
                }
            }
            graphics.text(font, state, x + w - 8 - font.width(state), y + 9, textColor, false);
        }

        // 技能没显示完 → 右侧滚动条 + 底部提示
        if (maxScrollRow() > 0) {
            int trackTop = HEADER;
            int trackHeight = visibleRows() * ROW_HEIGHT;
            int barX = x + w + 4;
            int thumbHeight = Math.max(12, trackHeight * visibleRows() / Math.max(1, skills.size()));
            int thumbY = trackTop + (trackHeight - thumbHeight) * scrollRow / Math.max(1, maxScrollRow());

            graphics.fill(barX, trackTop, barX + 2, trackTop + trackHeight, 0x44FFFFFF);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0xCCFFFFFF);
            graphics.centeredText(font,
                    Component.translatable("gui.corpseorigin.skill_tree.scroll_hint"),
                    width / 2, height - 14, 0xFF999999);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            int x = panelX();
            int w = panelWidth();
            int level = EvolutionManager.getLevel(ClientState.earnedPoints);
            for (int i = 0; i < skills.size(); i++) {
                int y = rowY(i);
                if (y < 0) {
                    continue;
                }
                boolean hovered = mouseX >= x && mouseX <= x + w
                        && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2;
                if (hovered) {
                    ISkill skill = skills.get(i);
                    boolean learned = ClientState.hasLearned(skill.getId().getPath());
                    boolean canLearn = !learned
                            && level >= skill.getRequiredLevel()
                            && ClientState.availablePoints >= skill.getCost()
                            && ClientState.learnedSkills.containsAll(
                            skill.getPrerequisites().stream().map(Identifier::getPath).toList());
                    if (canLearn) {
                        ClientPlayNetworking.send(new CorpsePayloads.LearnSkillC2S(
                                skill.getId().getPath()));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
