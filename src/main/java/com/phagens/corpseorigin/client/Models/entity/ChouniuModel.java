package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.npc.ChouniuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ChouniuModel extends GeoModel<ChouniuEntity> {

    @Override
    public ResourceLocation getModelResource(ChouniuEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/chouniu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChouniuEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/chouniu.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChouniuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/entity/chouniu.animation.json");
    }
}
