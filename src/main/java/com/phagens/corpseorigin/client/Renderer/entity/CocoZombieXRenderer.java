package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.CocoZombieXModel;
import com.phagens.corpseorigin.entity.zbrs.CocoZombieXEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CocoZombieXRenderer extends GeoEntityRenderer<CocoZombieXEntity> {

    public CocoZombieXRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CocoZombieXModel());
        this.shadowRadius = 0.8f; // 阴影大小
    }
}