package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.ZbWormModel;
import com.phagens.corpseorigin.entity.Animals.ZbWormEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZbWormRenderer extends GeoEntityRenderer<ZbWormEntity> {
    public ZbWormRenderer(EntityRendererProvider.Context context) {
        super(context, new ZbWormModel());
    }
}
