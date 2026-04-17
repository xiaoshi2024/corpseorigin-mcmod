package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.zbrs.CocoZombieXEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CocoZombieXModel extends GeoModel<CocoZombieXEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "geo/entity/coco_penguin_zbrx.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "textures/entity/coco_penguin_zbrx.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "animations/entity/coco_penguin_zbrx.animation.json");

    @Override
    public ResourceLocation getModelResource(CocoZombieXEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CocoZombieXEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CocoZombieXEntity animatable) {
        return ANIMATION;
    }
}