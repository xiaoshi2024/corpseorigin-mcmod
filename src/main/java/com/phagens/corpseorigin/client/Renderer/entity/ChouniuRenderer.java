package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.ChouniuModel;
import com.phagens.corpseorigin.entity.npc.ChouniuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChouniuRenderer extends GeoEntityRenderer<ChouniuEntity> {

    public ChouniuRenderer(EntityRendererProvider.Context context) {
        super(context, new ChouniuModel());
        this.shadowRadius = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(ChouniuEntity entity) {
        return super.getTextureLocation(entity);
    }
}
