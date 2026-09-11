package xiaoshi2022.corpseorigin.client.render.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 血莲宝灯·赫兹波形链条渲染器（双层发光版）
 */
public class BloodLotusLaserRenderer {

    private static final RenderType LASER_RENDER_TYPE = RenderTypes.lightning();

    // ==================== 双层颜色 ====================

    /** 外层光晕：暗红 */
    private static final int GLOW_R = 255;
    private static final int GLOW_G = 30;
    private static final int GLOW_B = 40;
    private static final float GLOW_ALPHA_MULT = 0.35F;

    /** 内层核心：亮白红 */
    private static final int CORE_R = 255;
    private static final int CORE_G = 180;
    private static final int CORE_B = 150;

    /** 外层宽度倍率 */
    private static final float GLOW_WIDTH_MULT = 3.0F;

    private static final Random RANDOM = new Random();

    // ==================== 赫兹波参数 ====================

    private static final double HERTZ_AMPLITUDE = 1.0;
    private static final double HERTZ_FREQUENCY = 3.5;
    private static final double HERTZ_JITTER = 0.25;
    private static final boolean HERTZ_HELICAL = false;

    /**
     * 提交赫兹波链条（双层渲染）
     */
    public static void submitLaser(PoseStack poseStack, SubmitNodeCollector collector,
                                   Vec3 start, Vec3 end, float width, float alpha,
                                   long seed) {
        Vec3 direction = end.subtract(start);
        double totalLength = direction.length();
        if (totalLength < 1.0E-6) {
            return;
        }

        Vec3 dirNorm = direction.normalize();
        Random rng = new Random(seed);

        List<Vec3> chainPoints = generateHertzChain(start, end, dirNorm, totalLength, rng);

        collector.submitCustomGeometry(poseStack, LASER_RENDER_TYPE, (pose, consumer) -> {
            Matrix4f matrix = pose.pose();

            // ✅ 第一层：外层光晕（粗、暗）
            for (int i = 0; i < chainPoints.size() - 1; i++) {
                Vec3 segStart = chainPoints.get(i);
                Vec3 segEnd = chainPoints.get(i + 1);

                double progress = (double) i / (chainPoints.size() - 1);
                int glowAlpha = (int) (255 * alpha * GLOW_ALPHA_MULT * (1.0F - (float) progress * 0.5F));

                drawSegmentColored(consumer, matrix, segStart, segEnd,
                        width * GLOW_WIDTH_MULT, glowAlpha,
                        GLOW_R, GLOW_G, GLOW_B);
            }

            // ✅ 第二层：内层核心（细、亮）
            for (int i = 0; i < chainPoints.size() - 1; i++) {
                Vec3 segStart = chainPoints.get(i);
                Vec3 segEnd = chainPoints.get(i + 1);

                double progress = (double) i / (chainPoints.size() - 1);
                int coreAlpha = (int) (255 * alpha * (1.0F - (float) progress * 0.3F));

                drawSegmentColored(consumer, matrix, segStart, segEnd,
                        width, coreAlpha,
                        CORE_R, CORE_G, CORE_B);
            }
        });
    }

    /**
     * 目标身上的缠绕环（双层渲染）
     */
    public static void submitCoil(PoseStack poseStack, SubmitNodeCollector collector,
                                  Vec3 center, double radius, float alpha, long seed) {
        Random rng = new Random(seed);

        // 先生成所有环的线段点
        List<Vec3[]> ringSegments = new ArrayList<>();

        for (int ring = 0; ring < 4; ring++) {
            double yOffset = (ring - 1.5) * 0.4;
            int segments = 20;

            for (int i = 0; i < segments; i++) {
                double angle1 = (double) i / segments * Math.PI * 2;
                double angle2 = (double) (i + 1) / segments * Math.PI * 2;

                double wave1 = Math.sin(angle1 * 5) * 0.2;
                double wave2 = Math.sin(angle2 * 5) * 0.2;

                double jitter1 = (rng.nextDouble() - 0.5) * 0.15;
                double jitter2 = (rng.nextDouble() - 0.5) * 0.15;

                Vec3 p1 = center.add(
                        Math.cos(angle1) * (radius + jitter1),
                        yOffset + wave1,
                        Math.sin(angle1) * (radius + jitter1)
                );
                Vec3 p2 = center.add(
                        Math.cos(angle2) * (radius + jitter2),
                        yOffset + wave2,
                        Math.sin(angle2) * (radius + jitter2)
                );

                ringSegments.add(new Vec3[]{p1, p2});
            }
        }

        collector.submitCustomGeometry(poseStack, LASER_RENDER_TYPE, (pose, consumer) -> {
            Matrix4f matrix = pose.pose();

            // ✅ 外层光晕
            for (Vec3[] seg : ringSegments) {
                int glowAlpha = (int) (255 * alpha * GLOW_ALPHA_MULT);
                drawSegmentColored(consumer, matrix, seg[0], seg[1],
                        0.45F, glowAlpha, GLOW_R, GLOW_G, GLOW_B);
            }

            // ✅ 内层核心
            for (Vec3[] seg : ringSegments) {
                int coreAlpha = (int) (255 * alpha);
                drawSegmentColored(consumer, matrix, seg[0], seg[1],
                        0.15F, coreAlpha, CORE_R, CORE_G, CORE_B);
            }
        });
    }

    /**
     * 生成赫兹波形路径
     */
    private static List<Vec3> generateHertzChain(Vec3 start, Vec3 end,
                                                 Vec3 dirNorm, double totalLength,
                                                 Random rng) {
        List<Vec3> points = new ArrayList<>();

        int segmentCount = Math.max(16, (int) (totalLength / 0.3));

        Vec3 perp1 = getPerpendicular(dirNorm);
        Vec3 perp2 = dirNorm.cross(perp1).normalize();

        double phase = rng.nextDouble() * Math.PI * 2;

        points.add(start);

        for (int i = 1; i < segmentCount; i++) {
            double t = (double) i / segmentCount;
            Vec3 basePoint = start.add(dirNorm.scale(totalLength * t));

            double envelope = Math.sin(t * Math.PI);

            double wave1 = Math.sin(t * Math.PI * 2 * HERTZ_FREQUENCY + phase)
                    * HERTZ_AMPLITUDE * envelope;

            Vec3 offset;
            if (HERTZ_HELICAL) {
                double wave2 = Math.cos(t * Math.PI * 2 * HERTZ_FREQUENCY + phase)
                        * HERTZ_AMPLITUDE * envelope;
                offset = perp1.scale(wave1).add(perp2.scale(wave2));
            } else {
                offset = perp1.scale(wave1);
            }

            offset = offset.add(perp1.scale((rng.nextDouble() - 0.5) * HERTZ_JITTER * envelope));
            offset = offset.add(perp2.scale((rng.nextDouble() - 0.5) * HERTZ_JITTER * envelope));

            points.add(basePoint.add(offset));
        }

        points.add(end);
        return points;
    }

    /**
     * ✅ 通用的画线段方法，支持自定义颜色
     */
    /**
     * 画一段激光（十字截面，立体光柱）
     */
    private static void drawSegmentColored(VertexConsumer consumer, Matrix4f matrix,
                                           Vec3 start, Vec3 end, float width, int alpha,
                                           int r, int g, int b) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }

        Vec3 dirNorm = direction.normalize();

        // ✅ 计算两个互相垂直的向量，组成十字截面
        Vec3 perp1 = getPerpendicular(dirNorm);
        Vec3 perp2 = dirNorm.cross(perp1).normalize();

        float halfWidth = width / 2.0F;

        // ✅ 画两个垂直的四边形面片
        drawQuad(consumer, matrix, start, end, perp1, halfWidth, alpha, r, g, b);
        drawQuad(consumer, matrix, start, end, perp2, halfWidth, alpha, r, g, b);
    }

    /**
     * 画一个四边形面片（由两个三角形组成）
     */
    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix,
                                 Vec3 start, Vec3 end, Vec3 perp, float halfWidth,
                                 int alpha, int r, int g, int b) {
        Vector3f perpVec = new Vector3f((float) perp.x, (float) perp.y, (float) perp.z)
                .mul(halfWidth);

        Vector3f p1 = new Vector3f((float) start.x, (float) start.y, (float) start.z).sub(perpVec);
        Vector3f p2 = new Vector3f((float) start.x, (float) start.y, (float) start.z).add(perpVec);
        Vector3f p3 = new Vector3f((float) end.x, (float) end.y, (float) end.z).add(perpVec);
        Vector3f p4 = new Vector3f((float) end.x, (float) end.y, (float) end.z).sub(perpVec);

        p1.mulPosition(matrix);
        p2.mulPosition(matrix);
        p3.mulPosition(matrix);
        p4.mulPosition(matrix);

        // 三角形1
        consumer.addVertex(p1.x(), p1.y(), p1.z()).setColor(r, g, b, alpha);
        consumer.addVertex(p2.x(), p2.y(), p2.z()).setColor(r, g, b, alpha);
        consumer.addVertex(p3.x(), p3.y(), p3.z()).setColor(r, g, b, alpha);

        // 三角形2
        consumer.addVertex(p1.x(), p1.y(), p1.z()).setColor(r, g, b, alpha);
        consumer.addVertex(p3.x(), p3.y(), p3.z()).setColor(r, g, b, alpha);
        consumer.addVertex(p4.x(), p4.y(), p4.z()).setColor(r, g, b, alpha);
    }

    private static Vec3 getPerpendicular(Vec3 dir) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 perp = dir.cross(up);
        if (perp.length() < 0.1) {
            perp = dir.cross(new Vec3(1, 0, 0));
        }
        return perp.normalize();
    }
}