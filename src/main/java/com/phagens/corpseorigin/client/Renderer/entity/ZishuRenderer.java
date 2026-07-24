package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.ZishuModel;
import com.phagens.corpseorigin.entity.npc.ZishuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZishuRenderer extends GeoEntityRenderer<ZishuEntity> {

    public ZishuRenderer(EntityRendererProvider.Context context) {
        super(context, new ZishuModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public ResourceLocation getTextureLocation(ZishuEntity entity) {
        return super.getTextureLocation(entity);
    }
}
