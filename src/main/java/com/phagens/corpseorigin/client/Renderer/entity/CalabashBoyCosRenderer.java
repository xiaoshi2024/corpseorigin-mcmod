package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.CalabashBoyCosModel;
import com.phagens.corpseorigin.entity.npc.CalabashBoyCosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CalabashBoyCosRenderer extends GeoEntityRenderer<CalabashBoyCosEntity> {

    public CalabashBoyCosRenderer(EntityRendererProvider.Context context) {
        super(context, new CalabashBoyCosModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(CalabashBoyCosEntity entity) {
        return super.getTextureLocation(entity);
    }
}