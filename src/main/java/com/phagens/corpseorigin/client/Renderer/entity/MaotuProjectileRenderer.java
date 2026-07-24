// 文件路径: src/main/java/com/phagens/corpseorigin/client/Renderer/entity/MaotuProjectileRenderer.java
package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.phagens.corpseorigin.client.Models.entity.MaotuProjectileModel;

import com.phagens.corpseorigin.entity.MaotuProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MaotuProjectileRenderer extends GeoEntityRenderer<MaotuProjectileEntity> {

    public MaotuProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, new MaotuProjectileModel());
        this.shadowRadius = 0.2f;
    }

    @Override
    public void render(MaotuProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MaotuProjectileEntity entity) {
        return super.getTextureLocation(entity);
    }
}
