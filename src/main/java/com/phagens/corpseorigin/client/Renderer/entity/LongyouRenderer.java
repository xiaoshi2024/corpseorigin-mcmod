package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.LongyouModel;
import com.phagens.corpseorigin.entity.LongyouEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LongyouRenderer extends GeoEntityRenderer<LongyouEntity> {
    public LongyouRenderer(EntityRendererProvider.Context context) {
        super(context, new LongyouModel());
    }

    @Override
    public ResourceLocation getTextureLocation(LongyouEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/longyou.png");
    }
}
