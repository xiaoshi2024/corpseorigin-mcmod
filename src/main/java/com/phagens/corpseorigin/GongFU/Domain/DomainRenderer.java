package com.phagens.corpseorigin.GongFU.Domain;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 领域实体渲染器 - 完全隐形，不渲染任何内容
 */
public class DomainRenderer extends EntityRenderer<DomainEntity> {
    
    public DomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DomainEntity entity) {
        return null; // 不需要纹理
    }

    @Override
    public void render(DomainEntity entity, float entityYaw, float partialTick, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 完全不渲染任何东西（隐形）
    }
}
