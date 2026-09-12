package xiaoshi2022.corpseorigin.client.aps;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import static xiaoshi2022.corpseorigin.client.aps.APSInkSceneManager.smoother;

/**
 * 诗仙剑·意境场景渲染器
 *
 * 结构：
 *   1. 天穹顶   —— 一块水平朝下的平面，贴 ink_sky.png 中心区域，抬头可见；
 *   2. 四方远山 —— 4 张图，围成一圈，就是场景的"侧壁"；
 *   3. 诗牌     —— 固定在河道上空，逐句自天穹落下，绕 Y 轴面向镜头。
 *
 * 关键修复：
 *   - 诗牌改成双面渲染（正面 + 反面各画一遍），避免单面剔除导致看到背面；
 *   - 全部用 entityTranslucentEmissive：自发光 + 半透明；
 *   - 所有自定义几何体在 submitCustomGeometry 的 lambda 内部取 p.pose()。
 */
@Environment(EnvType.CLIENT)
public final class APSInkSceneRenderer {

    private static final String NS = CorpseOrigin.MOD_ID;

    // ==================== 贴图 ====================

    private static final Identifier SKY =
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/ink_sky.png");

    private static final Identifier[] BACKGROUND = new Identifier[]{
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/background/background_1.png"),
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/background/background_2.png"),
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/background/background_3.png"),
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/background/background_4.png")
    };

    private static final Identifier[] POEM = new Identifier[]{
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/poem/poem_1.png"),
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/poem/poem_2.png"),
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/poem/poem_3.png"),
            Identifier.fromNamespaceAndPath(NS, "textures/effect/aps/poem/poem_4.png")
    };

    // ==================== 天穹顶 ====================

    /** 天穹顶距领域中心的高度 */
    private static final float SKY_TOP_Y = 80f;
    /** 天穹顶半径 */
    private static final float SKY_TOP_RADIUS = 120f;
    /** 顶面 UV 只用贴图中心区域 */
    private static final float SKY_UV_MIN = 0.25f;
    private static final float SKY_UV_MAX = 0.75f;

    // ==================== 四方远山 ====================

    /** 远山距领域中心水平距离 */
    private static final float BG_DIST = 120f;
    /** 每张远山宽度 */
    private static final float BG_WIDTH = 300f;
    /** 远山高度 */
    private static final float BG_HEIGHT = 180f;
    /** 远山底部偏移 */
    private static final float BG_BOTTOM_OFFSET = -40f;

    // ==================== 诗牌 ====================

    /** 诗牌宽度（贴图 700×2000，比例 0.35:1） */
    private static final float POEM_WIDTH = 20f;
    /** 诗牌高度（20 / 0.35 ≈ 57） */
    private static final float POEM_HEIGHT = 57f;
    /** 4 句之间的间距 */
    private static final float POEM_SPACING = 50f;
    /** 下落起点：相对领域中心的 Y 偏移 */
    private static final float POEM_START_Y = 100f;
    /** 下落终点：相对领域中心的 Y 偏移 */
    private static final float POEM_END_Y = 30f;

    private APSInkSceneRenderer() {}

    // ==================== 入口 ====================

    public static void render(PoseStack pose, SubmitNodeCollector collector, Vec3 cameraPos) {
        if (!APSInkSceneManager.shouldRender()) return;

        Vec3 center = APSInkSceneManager.center();
        float skyAlpha = APSInkSceneManager.getGroundAlpha();
        float sceneAlpha = APSInkSceneManager.getSceneAlpha();

        // 1. 天穹顶
        if (skyAlpha > 0.01f) {
            renderSkyTop(pose, collector, cameraPos, center, skyAlpha);
        }

        // 2. 四方远山
        if (sceneAlpha > 0.01f) {
            renderBackgrounds(pose, collector, cameraPos, center, sceneAlpha);
        }

        // 3. 诗牌（逐句从天上落下）
        renderPoems(pose, collector, cameraPos, center, sceneAlpha);
    }

    // ==================== 天穹顶 ====================

    private static void renderSkyTop(PoseStack pose, SubmitNodeCollector collector,
                                     Vec3 camera, Vec3 center, float alpha) {
        RenderType renderType = RenderTypes.entityTranslucentEmissive(SKY);

        final double cx = center.x;
        final double cz = center.z;
        final float topY = (float) (center.y + SKY_TOP_Y);
        final float r = SKY_TOP_RADIUS;
        final int a = 255;

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        collector.submitCustomGeometry(pose, renderType, (p, consumer) -> {
            Matrix4f mat = p.pose();

            vertex(consumer, mat, (float)(cx - r), topY, (float)(cz - r),
                    SKY_UV_MIN, SKY_UV_MAX, 255, 255, 255, a, 0f, -1f, 0f);
            vertex(consumer, mat, (float)(cx + r), topY, (float)(cz - r),
                    SKY_UV_MAX, SKY_UV_MAX, 255, 255, 255, a, 0f, -1f, 0f);
            vertex(consumer, mat, (float)(cx + r), topY, (float)(cz + r),
                    SKY_UV_MAX, SKY_UV_MIN, 255, 255, 255, a, 0f, -1f, 0f);
            vertex(consumer, mat, (float)(cx - r), topY, (float)(cz + r),
                    SKY_UV_MIN, SKY_UV_MIN, 255, 255, 255, a, 0f, -1f, 0f);
        });

        pose.popPose();
    }

    // ==================== 四方远山 ====================

    private static void renderBackgrounds(PoseStack pose, SubmitNodeCollector collector,
                                          Vec3 camera, Vec3 center, float alpha) {
        double[] angles = { 0.0, Math.PI / 2, Math.PI, Math.PI * 1.5 };

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        for (int i = 0; i < 4; i++) {
            Identifier tex = BACKGROUND[i % BACKGROUND.length];
            RenderType renderType = RenderTypes.entityTranslucentEmissive(tex);

            double angle = angles[i];
            final double bx = center.x + Math.cos(angle) * BG_DIST;
            final double bz = center.z + Math.sin(angle) * BG_DIST;
            final double byBottom = center.y + BG_BOTTOM_OFFSET;
            final double byTop = byBottom + BG_HEIGHT;

            Vec3 toCenter = new Vec3(center.x - bx, 0, center.z - bz).normalize();
            double sideX = -toCenter.z;
            double sideZ =  toCenter.x;

            float halfW = BG_WIDTH * 0.5f;
            final float sx = (float) (sideX * halfW);
            final float sz = (float) (sideZ * halfW);
            final float nx = (float) toCenter.x;
            final float nz = (float) toCenter.z;
            final int a = 255;

            collector.submitCustomGeometry(pose, renderType, (p, consumer) -> {
                Matrix4f mat = p.pose();
                vertex(consumer, mat, (float)(bx - sx), (float) byBottom, (float)(bz - sz),
                        0f, 1f, 255, 255, 255, a, nx, 0f, nz);
                vertex(consumer, mat, (float)(bx - sx), (float) byTop,    (float)(bz - sz),
                        0f, 0f, 255, 255, 255, a, nx, 0f, nz);
                vertex(consumer, mat, (float)(bx + sx), (float) byTop,    (float)(bz + sz),
                        1f, 0f, 255, 255, 255, a, nx, 0f, nz);
                vertex(consumer, mat, (float)(bx + sx), (float) byBottom, (float)(bz + sz),
                        1f, 1f, 255, 255, 255, a, nx, 0f, nz);
            });
        }

        pose.popPose();
    }

    // ==================== 诗牌（双面渲染） ====================

    private static void renderPoems(PoseStack pose, SubmitNodeCollector collector,
                                    Vec3 camera, Vec3 center, float alpha) {
        double dirX = APSInkSceneManager.RIVER_DIR_X;
        double dirZ = APSInkSceneManager.RIVER_DIR_Z;

        double spacing = POEM_SPACING;
        double startOffset = -(APSInkSceneManager.POEM_LINES - 1) * spacing * 0.5;

        float startY = (float) (center.y + POEM_START_Y);
        float endY   = (float) (center.y + POEM_END_Y);

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        for (int i = 0; i < APSInkSceneManager.POEM_LINES; i++) {
            float t = APSInkSceneManager.poemProgress(i);
            if (t < 0f) continue;

            float eased = smoother(t);
            final int a = (int) (eased * 255f);
            if (a <= 0) continue;

            double along = startOffset + i * spacing;
            final double px = center.x + dirX * along;
            final double pz = center.z + dirZ * along;
            final float baseY = Mth.lerp(eased, startY, endY);
            final float topY = baseY + POEM_HEIGHT;

            // 面朝相机（只绕 Y 轴）
            Vec3 base = new Vec3(px, baseY, pz);
            Vec3 toCam = new Vec3(camera.x - base.x, 0, camera.z - base.z);
            if (toCam.lengthSqr() < 1e-5) toCam = new Vec3(0, 0, 1);
            toCam = toCam.normalize();

            float halfW = POEM_WIDTH * 0.5f;
            final float sx = (float) (-toCam.z * halfW);
            final float sz = (float) ( toCam.x * halfW);
            final float nx = (float) toCam.x;
            final float nz = (float) toCam.z;

            RenderType renderType = RenderTypes.entityTranslucentEmissive(POEM[i]);

            collector.submitCustomGeometry(pose, renderType, (p, consumer) -> {
                Matrix4f mat = p.pose();

                // ---------- 正面（法线朝相机） ----------
                vertex(consumer, mat, (float)(px - sx), baseY, (float)(pz - sz),
                        0f, 1f, 255, 255, 255, a, nx, 0f, nz);
                vertex(consumer, mat, (float)(px - sx), topY,  (float)(pz - sz),
                        0f, 0f, 255, 255, 255, a, nx, 0f, nz);
                vertex(consumer, mat, (float)(px + sx), topY,  (float)(pz + sz),
                        1f, 0f, 255, 255, 255, a, nx, 0f, nz);
                vertex(consumer, mat, (float)(px + sx), baseY, (float)(pz + sz),
                        1f, 1f, 255, 255, 255, a, nx, 0f, nz);

                // ---------- 反面（绕序反转 + 法线反转） ----------
                vertex(consumer, mat, (float)(px + sx), baseY, (float)(pz + sz),
                        1f, 1f, 255, 255, 255, a, -nx, 0f, -nz);
                vertex(consumer, mat, (float)(px + sx), topY,  (float)(pz + sz),
                        1f, 0f, 255, 255, 255, a, -nx, 0f, -nz);
                vertex(consumer, mat, (float)(px - sx), topY,  (float)(pz - sz),
                        0f, 0f, 255, 255, 255, a, -nx, 0f, -nz);
                vertex(consumer, mat, (float)(px - sx), baseY, (float)(pz - sz),
                        0f, 1f, 255, 255, 255, a, -nx, 0f, -nz);
            });
        }

        pose.popPose();
    }

    // ==================== 通用顶点 ====================

    private static void vertex(VertexConsumer consumer, Matrix4f mat,
                               float x, float y, float z,
                               float u, float v,
                               int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        Vector3f pos = new Vector3f(x, y, z).mulPosition(mat);
        consumer.addVertex(pos.x(), pos.y(), pos.z())
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(nx, ny, nz);
    }
}