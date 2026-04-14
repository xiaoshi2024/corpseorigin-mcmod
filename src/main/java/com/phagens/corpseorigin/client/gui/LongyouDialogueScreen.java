package com.phagens.corpseorigin.client.gui;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.LongyouDialoguePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

public class LongyouDialogueScreen extends Screen {
    public static final ResourceLocation ICON_TEXTURES = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/gui.png");
    private final Player player = Objects.requireNonNull(Minecraft.getInstance().player);
    private List<String> dialogAnswers;
    private String dialogAnswerHover;
    private List<FormattedCharSequence> dialogQuestionText;
    private String dialogQuestionId;

    public LongyouDialogueScreen() {
        super(Component.literal("Longyou Dialogue"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Objects.requireNonNull(this.minecraft).setScreen(null);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        drawDialogue(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double posX, double posY, int button) {
        super.mouseClicked(posX, posY, button);

        // 处理对话选项点击
        if (button == 0 && dialogAnswerHover != null && dialogQuestionText != null) {
            // 发送选择到服务器
            LongyouDialoguePacket packet = new LongyouDialoguePacket(dialogAnswerHover);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
            
            // 关闭对话框（类似MCA的效果，点击一次就关闭）
            this.onClose();
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyChar, int keyCode, int unknown) {
        if (keyChar == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    private void drawDialogue(GuiGraphics context, int mouseX, int mouseY) {
        if (dialogQuestionText != null) {
            // 背景
            context.fill(width / 2 - 85, height / 2 - 50 - 10 * dialogQuestionText.size(), width / 2 + 85,
                    height / 2 - 30 + 10 * dialogAnswers.size(), 0x77000000);

            // 问题
            int i = -dialogQuestionText.size();
            for (FormattedCharSequence t : dialogQuestionText) {
                i++;
                context.drawString(font, t, width / 2 - font.width(t) / 2, height / 2 - 50 + i * 10, 0xFFFFFFFF);
            }
            dialogAnswerHover = null;

            // 分隔线
            context.hLine(width / 2 - 75, width / 2 + 75, height / 2 - 40, 0xAAFFFFFF);

            // 答案选项
            int y = height / 2 - 35;
            for (String a : dialogAnswers) {
                boolean hover = hoveringOver(width / 2 - 100, y - 3, 200, 10, mouseX, mouseY);
                context.drawCenteredString(font, Component.literal(a), width / 2, y, hover ? 0xFFD7D784 : 0xAAFFFFFF);
                if (hover) {
                    dialogAnswerHover = a;
                }
                y += 10;
            }
        }
    }

    private boolean hoveringOver(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    public void setDialogue(String question, List<String> answers) {
        this.dialogQuestionId = question;
        this.dialogAnswers = answers;
        // 延迟初始化dialogQuestionText，因为font对象在Screen的init()方法中才初始化
        if (font != null) {
            this.dialogQuestionText = font.split(Component.literal(question), 160);
        }
    }

    @Override
    protected void init() {
        super.init();
        // 当font对象初始化后，检查是否需要初始化dialogQuestionText
        if (dialogQuestionId != null && dialogQuestionText == null) {
            this.dialogQuestionText = font.split(Component.literal(dialogQuestionId), 160);
        }
    }
}
