package com.phagens.corpseorigin.client.gui.radialmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 轮盘菜单 GUI 渲染类 - 专业版
 * 使用分层渲染和专业渲染技术
 */
public abstract class GuiRadialMenu<T> extends Screen {

    protected static final int MAX_SLOTS = 12;
    protected static final float OPEN_ANIMATION_LENGTH = 0.15f;
    protected static final float INNER_RADIUS = 30.0f;
    protected static final float OUTER_RADIUS = 100.0f;
    protected static final float ANIMATED_RADIUS = 120.0f;

    // 颜色常量 (ARGB)
    protected static final int COLOR_BACKGROUND = 0xCC2A2A3E;
    protected static final int COLOR_HIGHLIGHT = 0xCC4488FF;
    protected static final int COLOR_SELECTED = 0xCC66AAFF;
    protected static final int COLOR_COOLDOWN = 0xCCAA4444;
    protected static final int COLOR_DISABLED = 0xCC444444;

    protected boolean closing = false;
    protected final RadialMenu<T> radialMenu;
    protected final List<IRadialMenuSlot<T>> radialMenuSlots;

    protected float totalTime = 0;
    protected int selectedItem = -1;

    protected final Minecraft minecraft;
    protected int centerX;
    protected int centerY;

    // 渲染层
    protected static final float Z_BACKGROUND = -100.0f;
    protected static final float Z_SLICES = 0.0f;
    protected static final float Z_HIGHLIGHT = 50.0f;
    protected static final float Z_ICONS = 100.0f;
    protected static final float Z_TEXT = 150.0f;

    // 纹理参数（可选）
    protected ResourceLocation sliceTexture;
    protected ResourceLocation centerTexture;
    protected ResourceLocation highlightTexture;

    public GuiRadialMenu(RadialMenu<T> radialMenu) {
        this(radialMenu, null, null, null);
    }

    public GuiRadialMenu(RadialMenu<T> radialMenu, ResourceLocation sliceTexture,
                         ResourceLocation centerTexture, ResourceLocation highlightTexture) {
        super(Component.empty());
        this.radialMenu = radialMenu;
        this.radialMenuSlots = radialMenu.getSlots();
        this.minecraft = Minecraft.getInstance();
        this.sliceTexture = sliceTexture;
        this.centerTexture = centerTexture;
        this.highlightTexture = highlightTexture;
    }

    @Override
    protected void init() {
        super.init();
        centerX = this.width / 2;
        centerY = this.height / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染半透明背景
        renderBackground(graphics, mouseX, mouseY, partialTicks);

        // 更新动画时间
        totalTime += partialTicks / 20.0f;

        // 计算动画进度（使用缓动函数）
        float animProgress = Mth.clamp(totalTime / OPEN_ANIMATION_LENGTH, 0.0f, 1.0f);
        animProgress = 1.0f - (float)Math.pow(1.0f - animProgress, 3.0f);

        // 计算当前半径
        float radiusIn = INNER_RADIUS * animProgress;
        float radiusOut = OUTER_RADIUS * animProgress;

        // 计算鼠标位置和选中项
        updateSelectedItem(mouseX, mouseY, radiusIn, radiusOut);

        // 设置渲染状态
        setupRenderState();

        // 渲染轮盘扇区
        renderSlices(graphics, radiusIn, radiusOut);

        // 渲染中心
        renderCenter(graphics, radiusIn);

        // 渲染选中项信息
        if (selectedItem != -1 && selectedItem < radialMenuSlots.size()) {
            renderSelectedInfo(graphics);
        }

        // 恢复渲染状态
        clearRenderState();

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    protected void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    protected void clearRenderState() {
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 使用半透明黑色背景
        graphics.fill(0, 0, this.width, this.height, 0xCC1A1A2E);
    }

    protected void updateSelectedItem(int mouseX, int mouseY, float radiusIn, float radiusOut) {
        if (closing) {
            selectedItem = -1;
            return;
        }

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < radiusIn || distance > radiusOut) {
            selectedItem = -1;
            return;
        }

        int slotCount = radialMenuSlots.size();
        if (slotCount == 0) {
            selectedItem = -1;
            return;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        // 调整角度使-90度(顶部)为0索引，顺时针增加
        angle = (angle + 90 + 360) % 360;

        float anglePerSlot = 360.0f / slotCount;
        selectedItem = (int)(angle / anglePerSlot);

        if (selectedItem >= slotCount) {
            selectedItem = slotCount - 1;
        }
    }

    protected void renderSlices(GuiGraphics graphics, float radiusIn, float radiusOut) {
        int slotCount = radialMenuSlots.size();
        if (slotCount == 0) return;

        float anglePerSlot = 360.0f / slotCount;

        for (int i = 0; i < slotCount; i++) {
            // 修正角度计算，使索引0在顶部(-90度)，与图标绘制一致
            float centerAngle = i * anglePerSlot - 90.0f;
            float startAngle = centerAngle - anglePerSlot / 2;
            float endAngle = centerAngle + anglePerSlot / 2;

            boolean isSelected = (i == selectedItem);
            IRadialMenuSlot<T> slot = radialMenuSlots.get(i);

            // 绘制扇区背景
            drawSlice(graphics, slot, false, centerX, centerY, Z_SLICES,
                    radiusIn, radiusOut, startAngle, endAngle, i);

            // 绘制高亮
            if (isSelected) {
                drawSlice(graphics, slot, true, centerX, centerY, Z_HIGHLIGHT,
                        radiusIn - 2, radiusOut + 2, startAngle, endAngle, i);
            }

            // 绘制图标
            drawSlotIcon(graphics, slot, i, slotCount, radiusIn, radiusOut);
        }
    }

    protected void drawSlice(GuiGraphics graphics, IRadialMenuSlot<T> slot, boolean highlighted,
                             float x, float y, float z, float radiusIn, float radiusOut,
                             float startAngle, float endAngle, int index) {

        int segments = Math.max(16, (int)((endAngle - startAngle) / 5.0f));
        float angleStep = (float)Math.toRadians((endAngle - startAngle) / segments);

        float startRad = (float)Math.toRadians(startAngle);

        int color = getSliceColor(slot, highlighted, index);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        BufferBuilder buffer = (BufferBuilder) bufferSource.getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();

        for (int i = 0; i < segments; i++) {
            float angle1 = startRad + i * angleStep;
            float angle2 = startRad + (i + 1) * angleStep;

            float x1 = x + radiusIn * Mth.cos(angle1);
            float y1 = y + radiusIn * Mth.sin(angle1);
            float x2 = x + radiusOut * Mth.cos(angle1);
            float y2 = y + radiusOut * Mth.sin(angle1);
            float x3 = x + radiusOut * Mth.cos(angle2);
            float y3 = y + radiusOut * Mth.sin(angle2);
            float x4 = x + radiusIn * Mth.cos(angle2);
            float y4 = y + radiusIn * Mth.sin(angle2);

            // 三角形1
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y1, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z).setColor(r, g, b, a);

            // 三角形2
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x3, y3, z).setColor(r, g, b, a);

            // 三角形3
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x3, y3, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x4, y4, z).setColor(r, g, b, a);

            // 三角形4
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x4, y4, z).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y1, z).setColor(r, g, b, a);
        }

        bufferSource.endBatch();
    }

    protected int getSliceColor(IRadialMenuSlot<T> slot, boolean highlighted, int index) {
        if (highlighted) {
            return COLOR_HIGHLIGHT;
        }
        return COLOR_BACKGROUND;
    }

    protected void drawSlotIcon(GuiGraphics graphics, IRadialMenuSlot<T> slot,
                                int index, int totalSlots, float radiusIn, float radiusOut) {
        float angle = index * (360.0f / totalSlots) - 90.0f;
        float radians = (float)Math.toRadians(angle);

        float iconRadius = (radiusIn + radiusOut) * 0.5f;
        int iconX = (int)(centerX + iconRadius * Mth.cos(radians) - 8);
        int iconY = (int)(centerY + iconRadius * Mth.sin(radians) - 8);

        // 保存当前pose
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z_ICONS);

        // 绘制主图标
        drawIcon(graphics, slot.primarySlotIcon(), iconX, iconY);

        // 绘制次要图标
        if (slot.hasSecondaryIcons()) {
            List<T> secondaryIcons = slot.secondarySlotIcons();
            for (int i = 0; i < Math.min(secondaryIcons.size(), 3); i++) {
                float offsetAngle = radians + (i - 1) * 0.3f;
                int secX = (int)(centerX + iconRadius * 0.7f * Mth.cos(offsetAngle) - 4);
                int secY = (int)(centerY + iconRadius * 0.7f * Mth.sin(offsetAngle) - 4);
                drawSecondaryIcon(graphics, secondaryIcons.get(i), secX, secY);
            }
        }

        graphics.pose().popPose();
    }

    protected abstract void drawIcon(GuiGraphics graphics, T icon, int x, int y);

    protected abstract void drawSecondaryIcon(GuiGraphics graphics, T icon, int x, int y);

    protected void renderCenter(GuiGraphics graphics, float radius) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z_TEXT);

        // 绘制中心圆
        graphics.fill(centerX - 20, centerY - 20, centerX + 20, centerY + 20, 0xCC000000);
        graphics.renderOutline(centerX - 20, centerY - 20, 40, 40, 0xFFFFFFFF);

        // 绘制中心文字
        if (selectedItem != -1 && selectedItem < radialMenuSlots.size()) {
            IRadialMenuSlot<T> slot = radialMenuSlots.get(selectedItem);
            Component name = slot.getName();
            int textWidth = minecraft.font.width(name);
            graphics.drawString(minecraft.font, name,
                    centerX - textWidth / 2, centerY - 4, 0xFFFFFF, true);
        }

        graphics.pose().popPose();
    }

    protected void renderSelectedInfo(GuiGraphics graphics) {
        if (selectedItem >= 0 && selectedItem < radialMenuSlots.size()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, Z_TEXT);

            IRadialMenuSlot<T> slot = radialMenuSlots.get(selectedItem);
            Component name = slot.getName();

            int textWidth = minecraft.font.width(name);
            int x = (this.width - textWidth) / 2 - 5;
            int y = this.height - 45;

            graphics.fill(x - 5, y - 5, x + textWidth + 15, y + 20, 0xCC000000);
            graphics.renderOutline(x - 5, y - 5, textWidth + 20, 25, 0xFFFFFFFF);
            graphics.drawString(minecraft.font, name, x, y, 0xFFFFFF, true);

            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            this.onClose();
            return true;
        } else if (button == 0 && selectedItem != -1) {
            radialMenu.select(selectedItem);
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 数字键 1-9 直接选择并释放对应技能
        int number = keyCode - 48; // GLFW_KEY_1 = 49
        if (number >= 1 && number <= Math.min(radialMenuSlots.size(), 9)) {
            selectedItem = number - 1;
            radialMenu.select(selectedItem);
            this.onClose();
            return true;
        }

        // ESC 键关闭
        if (keyCode == 256) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closing = true;
        super.onClose();
    }
}