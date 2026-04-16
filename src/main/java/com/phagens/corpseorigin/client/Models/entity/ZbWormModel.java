package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.entity.Animals.ZbWormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class ZbWormModel extends DefaultedEntityGeoModel<ZbWormEntity> {
    public ZbWormModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "zb_worm"));
    }

    @Override
    public ResourceLocation getModelResource(ZbWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/zb_worm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZbWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/zb_worm.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZbWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/entity/zb_worm.animation.json");
    }
}
