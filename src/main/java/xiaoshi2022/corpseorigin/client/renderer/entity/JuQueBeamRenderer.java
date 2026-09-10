package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;

import static xiaoshi2022.corpseorigin.CorpseOrigin.MOD_ID;

public class JuQueBeamRenderer extends EntityRenderer<JuQueBeamEntity, EntityRenderState> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            MOD_ID, "textures/entity/juque_beam.png");

    public JuQueBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);

        poseStack.pushPose();

        // ⚠️ 这里拿不到 entity，旋转数据需要从 state 里读。
        // 如果暂时不想做自定义 RenderState，就先只保留固定的 90 度旋转，
        // 不跟随实体朝向（视觉上剑气是个固定方向的贴图）。
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(1.5F, 0.5F, 0.5F);

        RenderType renderType = RenderTypes.entityCutout(TEXTURE);  // ✅ 改这里

        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            var matrix = pose.pose();
            vertex(consumer, matrix, -0.5F, -0.25F, 0.0F, 0.0F, 1.0F);
            vertex(consumer, matrix,  0.5F, -0.25F, 0.0F, 1.0F, 1.0F);
            vertex(consumer, matrix,  0.5F,  0.75F, 0.0F, 1.0F, 0.0F);
            vertex(consumer, matrix, -0.5F,  0.75F, 0.0F, 0.0F, 0.0F);
        });

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer,
                               org.joml.Matrix4f matrix,
                               float x, float y, float z,
                               float u, float v) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0F, 1.0F, 0.0F);
    }


}