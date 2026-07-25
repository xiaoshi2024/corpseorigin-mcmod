package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.npc.CalabashBoyCosEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CalabashBoyCosModel extends GeoModel<CalabashBoyCosEntity> {

    @Override
    public ResourceLocation getModelResource(CalabashBoyCosEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/calabash_boy_cos.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CalabashBoyCosEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/calabash_boy_cos.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CalabashBoyCosEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/entity/calabash_boy_cos.animation.json");
    }
}