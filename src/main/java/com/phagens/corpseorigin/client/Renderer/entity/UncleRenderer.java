package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.UncleModel;
import com.phagens.corpseorigin.entity.npc.UncleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UncleRenderer extends GeoEntityRenderer<UncleEntity> {

    public UncleRenderer(EntityRendererProvider.Context context) {
        super(context, new UncleModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(UncleEntity entity) {
        return super.getTextureLocation(entity);
    }
}