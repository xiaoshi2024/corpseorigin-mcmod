package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;

/** Continuous, emissive qi blade and wake using the shared qi mist texture. */
public class FlyingGreatSwordRenderer
        extends EntityRenderer<FlyingGreatSwordEntity, FlyingGreatSwordRenderState> {

    public FlyingGreatSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;   // 剑气没有实体，不投阴影
    }

    @Override
    public FlyingGreatSwordRenderState createRenderState() {
        return new FlyingGreatSwordRenderState();
    }

    @Override
    public void extractRenderState(FlyingGreatSwordEntity entity,
                                   FlyingGreatSwordRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.syncedYaw = entity.getSyncedYaw();
        state.syncedPitch = entity.getSyncedPitch();
        state.renderScale = entity.getRenderScale();
        state.phase = entity.getPhase();
        state.qiAge = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(FlyingGreatSwordRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.phase > 1 || state.renderScale <= .001f) return;
        final double radius = .42 * state.renderScale;
        final double age = state.qiAge;
        final boolean charging = state.phase == 0;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.syncedYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.syncedPitch));
        collector.submitCustomGeometry(poseStack,
                RenderTypes.entityTranslucentEmissive(CorpseOrigin.id("textures/effect/qi_mist.png")),
                (pose, out) -> {
                    // A broad crescent with a bright inner layer and a tapering wake.
                    // Render both windings so the qi is visible from either side.
                    for (int layer = 0; layer < 3; layer++) {
                        for (int segment = 0; segment < 32; segment++) {
                            for (int face = 0; face < 2; face++) {
                                for (int vertex = 0; vertex < 4; vertex++) {
                                    int corner = face == 0 ? vertex : 3 - vertex;
                                    double t = (segment + (corner >= 2 ? 1 : 0)) / 32.0;
                                    double angle = -1.75 + 3.5 * t;
                                    boolean rear = corner == 1 || corner == 2;
                                    double edge = Math.sin(Math.PI * t);
                                    double thickness = (layer == 2 ? .12 : .35) * edge;
                                    double r = radius * (1 - (rear ? thickness : 0));
                                    double x = Math.sin(angle) * r;
                                    double y = .06 * Math.sin(angle * 4 - age * .3) * edge;
                                    double z = Math.cos(angle) * r - (rear ? radius * (charging ? .3 : 1.4) * edge : 0);
                                    // Slightly fanned surfaces give the blade volume from head-on views.
                                    y += (layer - 1) * (.08 * edge + z * .22);
                                    int alpha = (int)((rear ? 28 : layer == 2 ? 210 : 110) * edge);
                                    out.addVertex(pose.pose(), (float)x, (float)y, (float)z)
                                            .setColor(layer == 2 ? 245 : 180, layer == 2 ? 252 : 220, 255, alpha)
                                            .setUv((float)t, rear ? 1f : 0f)
                                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
                                }
                            }
                        }
                    }
                });
        poseStack.popPose();
    }
}
