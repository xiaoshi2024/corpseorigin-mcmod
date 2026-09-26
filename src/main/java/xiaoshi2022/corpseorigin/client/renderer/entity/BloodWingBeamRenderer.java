package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.entity.BloodWingBeamEntity;

/** Emissive blood-red crescent perpendicular to its flight direction. */
public final class BloodWingBeamRenderer extends EntityRenderer<BloodWingBeamEntity, BloodWingBeamRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "corpseorigin", "textures/entity/juque_beam.png");
    public static final class State extends EntityRenderState {
        float yaw, pitch;
    }
    public BloodWingBeamRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(BloodWingBeamEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var direction = entity.getDeltaMovement().normalize();
        state.yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        state.pitch = (float) -Math.toDegrees(Math.asin(Math.clamp(direction.y, -1, 1)));
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poses, collector, camera);
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poses.mulPose(Axis.XP.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(poses, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, vertices) -> {
            for (int segment = 0; segment < 16; segment++) {
                double a = -Math.PI * .8 + segment * Math.PI * 1.6 / 16;
                double b = -Math.PI * .8 + (segment + 1) * Math.PI * 1.6 / 16;
                point(vertices, pose.pose(), a, 1.1f, 0, 0);
                point(vertices, pose.pose(), b, 1.1f, 1, 0);
                point(vertices, pose.pose(), b, .72f, 1, 1);
                point(vertices, pose.pose(), a, .72f, 0, 1);
            }
        });
        poses.popPose();
    }
    private static void point(VertexConsumer out, org.joml.Matrix4f matrix, double angle, float radius, float u, float v) {
        out.addVertex(matrix, (float) Math.sin(angle) * radius, (float) Math.cos(angle) * radius, 0)
                .setColor(255, 35, 60, 230).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(0, 0, 1);
    }
}
