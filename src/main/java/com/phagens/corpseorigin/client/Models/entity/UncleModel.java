package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.npc.UncleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UncleModel extends GeoModel<UncleEntity> {

    @Override
    public ResourceLocation getModelResource(UncleEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/uncles.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(UncleEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/uncles.png");
    }

    @Override
    public ResourceLocation getAnimationResource(UncleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/entity/uncles.animation.json");
    }
}