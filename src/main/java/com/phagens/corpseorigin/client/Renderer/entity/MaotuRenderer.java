package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.phagens.corpseorigin.client.Models.entity.MaotuModel;
import com.phagens.corpseorigin.entity.npc.MaotuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MaotuRenderer extends GeoEntityRenderer<MaotuEntity> {

    public MaotuRenderer(EntityRendererProvider.Context context) {
        super(context, new MaotuModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(MaotuEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MaotuEntity entity) {
        return super.getTextureLocation(entity);
    }
}
