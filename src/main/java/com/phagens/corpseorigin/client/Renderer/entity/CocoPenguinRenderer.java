package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.CocoPenguinModel;
import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CocoPenguinRenderer extends GeoEntityRenderer<CocoPenguinEntity> {
    public CocoPenguinRenderer(EntityRendererProvider.Context context) {
        super(context, new CocoPenguinModel());
    }
}
