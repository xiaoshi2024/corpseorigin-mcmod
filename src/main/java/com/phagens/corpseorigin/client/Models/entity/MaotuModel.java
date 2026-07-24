package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.npc.MaotuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MaotuModel extends GeoModel<MaotuEntity> {

    @Override
    public ResourceLocation getModelResource(MaotuEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/maotu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MaotuEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/maotu.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MaotuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/entity/maotu.animation.json");
    }
}
