package com.phagens.corpseorigin.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 技能树连接线渲染器
 */
public class ConnectionRenderer {

    private static final int LINE_WIDTH = 2;

    public static void renderConnection(GuiGraphics graphics,
                                        int x1, int y1, int x2, int y2,
                                        int color, ResourceLocation texture) {
        var bufferSource = graphics.bufferSource();
        var buffer = bufferSource.getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();

        // 计算方向
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.1f) return;

        // 归一化
        float nx = dy / length;  // 垂直向量
        float ny = -dx / length;

        // 计算四个顶点
        float halfWidth = LINE_WIDTH / 2f;
        float x1a = x1 + nx * halfWidth;
        float y1a = y1 + ny * halfWidth;
        float x1b = x1 - nx * halfWidth;
        float y1b = y1 - ny * halfWidth;
        float x2a = x2 + nx * halfWidth;
        float y2a = y2 + ny * halfWidth;
        float x2b = x2 - nx * halfWidth;
        float y2b = y2 - ny * halfWidth;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        // 绘制四边形
        buffer.addVertex(matrix, x1a, y1a, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1b, y1b, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2b, y2b, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2a, y2a, 0).setColor(r, g, b, a);
    }
}