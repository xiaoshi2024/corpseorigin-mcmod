package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.GuigunModel;
import com.phagens.corpseorigin.entity.GuigunEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GuigunRenderer extends GeoEntityRenderer<GuigunEntity> {
    public GuigunRenderer(EntityRendererProvider.Context context) {
        super(context, new GuigunModel());
    }

    @Override
    public ResourceLocation getTextureLocation(GuigunEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/guigun.png");
    }
}