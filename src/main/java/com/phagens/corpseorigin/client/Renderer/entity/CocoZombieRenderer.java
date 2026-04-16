package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.CocoZombieModel;
import com.phagens.corpseorigin.entity.Animals.CocoZombieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CocoZombieRenderer extends GeoEntityRenderer<CocoZombieEntity> {
    public CocoZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new CocoZombieModel());
    }
}
