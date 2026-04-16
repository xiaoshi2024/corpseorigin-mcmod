package com.phagens.corpseorigin.client.Models.item;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.zbritem.ZbWormitem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZbWormitemModel extends GeoModel<ZbWormitem> {
    
    @Override
    public ResourceLocation getModelResource(ZbWormitem object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/item/zb_worm_item.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(ZbWormitem object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/item/zb_worm_item.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(ZbWormitem animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/item/zb_worm_item.animation.json");
    }
}