package com.phagens.corpseorigin.GongFU.FaXiang.Geo.Renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import com.phagens.corpseorigin.GongFU.FaXiang.Geo.Model.FaxiangModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FaxiangRenderer extends GeoEntityRenderer<FaxiangEntity> {
    public FaxiangRenderer(EntityRendererProvider.Context context) {
        super(context, new FaxiangModel());
    }

    @Override
    public ResourceLocation getTextureLocation(FaxiangEntity entity) {
        return entity.getTextureResource();
    }

    @Override
    public void render(FaxiangEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float scale = (float) entity.getScale();
        poseStack.scale(scale, scale, scale);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}
