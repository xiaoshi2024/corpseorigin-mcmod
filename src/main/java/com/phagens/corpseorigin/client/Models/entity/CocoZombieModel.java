package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.entity.Animals.CocoZombieEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class CocoZombieModel extends DefaultedEntityGeoModel<CocoZombieEntity> {
    public CocoZombieModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "coco_penguin_zbr"));
    }

    @Override
    public ResourceLocation getModelResource(CocoZombieEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/coco_penguin_zbr.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CocoZombieEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/coco_penguin_zbr.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CocoZombieEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/entity/coco_penguin_zbr.animation.json");
    }
}
