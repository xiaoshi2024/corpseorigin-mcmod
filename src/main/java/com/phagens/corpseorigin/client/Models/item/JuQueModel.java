package com.phagens.corpseorigin.client.Models.item;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.JuQue;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class JuQueModel extends GeoModel<JuQue> {

    @Override
    public ResourceLocation getModelResource(JuQue object) {
        String modelName = "ming_juque";
        if ("tw".equals(object.getVariant())) {
            modelName = "ming_juque_tw";
        }
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/item/" + modelName + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JuQue object) {
        String textureName = "ming_juque";
        if ("tw".equals(object.getVariant())) {
            textureName = "ming_juque_tw";
        }
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/item/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(JuQue animatable) {
        String animationName = "ming_juque";
        if ("tw".equals(animatable.getVariant())) {
            animationName = "ming_juque_tw";
        }
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/item/" + animationName + ".animation.json");
    }
}
