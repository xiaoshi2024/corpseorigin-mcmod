package com.phagens.corpseorigin.entity.runEntityBeam.Renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phagens.corpseorigin.entity.runEntityBeam.Entity.LaserBeamEntity;
import com.phagens.corpseorigin.entity.runEntityBeam.Model.LaserBeamModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class LaserBeamRenderer extends GeoEntityRenderer<LaserBeamEntity> {
    public LaserBeamRenderer(EntityRendererProvider.Context context) {
        super(context, new LaserBeamModel());
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBeamEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/laser_beam.png");
    }

    @Override
    public void render(LaserBeamEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float length = entity.getCurrentLength();
        if (length > 0) {
            poseStack.scale(1.0f, 1.0f, length);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
    }

