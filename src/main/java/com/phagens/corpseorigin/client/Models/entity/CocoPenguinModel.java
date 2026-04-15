package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class CocoPenguinModel extends DefaultedEntityGeoModel<CocoPenguinEntity> {
    public CocoPenguinModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "coco_penguin"));
    }

    @Override
    public ResourceLocation getModelResource(CocoPenguinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/coco_penguin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CocoPenguinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/coco_penguin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CocoPenguinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/entity/coco_penguin.animation.json");
    }
}
