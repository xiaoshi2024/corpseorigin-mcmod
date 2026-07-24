package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.npc.ZishuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZishuModel extends GeoModel<ZishuEntity> {

    @Override
    public ResourceLocation getModelResource(ZishuEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/zishu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZishuEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/zishu.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZishuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/entity/zishu.animation.json");
    }
}
