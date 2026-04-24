package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.phagens.corpseorigin.entity.JuQueBeamEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class JuQueBeamRenderer extends EntityRenderer<JuQueBeamEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/juque_beam.png");

    public JuQueBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(JuQueBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // 根据实体朝向调整旋转
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(1.5F, 0.5F, 0.5F);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        
        // 绘制剑气四边形
        vertex(vertexConsumer, matrix4f, -0.5F, -0.25F, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(vertexConsumer, matrix4f, 0.5F, -0.25F, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(vertexConsumer, matrix4f, 0.5F, 0.75F, 0.0F, 1.0F, 0.0F, packedLight);
        vertex(vertexConsumer, matrix4f, -0.5F, 0.75F, 0.0F, 0.0F, 0.0F, packedLight);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, int packedLight) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(JuQueBeamEntity entity) {
        return TEXTURE;
    }
}