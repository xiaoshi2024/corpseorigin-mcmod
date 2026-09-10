package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.List;

/**
 * 技能进化树 GUI - 列出当前角色全部技能，点击学习
 */
public class SkillTreeScreen extends Screen {

    private List<ISkill> skills = List.of();
    private static final int ROW_HEIGHT = 30;
    private int top;

    public SkillTreeScreen() {
        super(Component.translatable("gui.corpseorigin.skill_tree"));
    }

    @Override
    protected void init() {
        skills = ClientCharacterCache.getCharacterSkills();
        top = 40;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(font, title, width / 2, 15, 0xFFFFFF);
        graphics.centeredText(font,
                Component.translatable("gui.corpseorigin.skill_tree.points",
                        ClientState.availablePoints, ClientState.earnedPoints),
                width / 2, 28, 0xFF55FF55);

        int x = width / 2 - 140;
        int y = top;
        int level = EvolutionManager.getLevel(ClientState.earnedPoints);

        for (ISkill skill : skills) {
            boolean learned = ClientState.hasLearned(skill.getId().getPath());
            boolean hovered = mouseX >= x && mouseX <= x + 280
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
            graphics.fill(x, y, x + 280, y + ROW_HEIGHT - 2, bg);
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
                state = Component.translatable("gui.corpseorigin.skill_tree.locked").getString();
            }
            graphics.text(font, state, x + 272 - font.width(state), y + 9, textColor, false);

            y += ROW_HEIGHT;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            int x = width / 2 - 140;
            int y = top;
            int level = EvolutionManager.getLevel(ClientState.earnedPoints);
            for (ISkill skill : skills) {
                boolean hovered = mouseX >= x && mouseX <= x + 280
                        && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2;
                if (hovered) {
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
                y += ROW_HEIGHT;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}