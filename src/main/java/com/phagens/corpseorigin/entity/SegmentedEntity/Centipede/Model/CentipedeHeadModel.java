package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Model;

import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeHead;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CentipedeHeadModel extends GeoModel<CentipedeHead> {
    
    @Override
    public ResourceLocation getModelResource(CentipedeHead animatable) {
        // TODO: 替换为实际的模型路径
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "geo/entity/coco_penguin_zbrx.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CentipedeHead animatable) {
        // TODO: 替换为实际的纹理路径
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/coco_penguin_zbrx.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CentipedeHead animatable) {
        // TODO: 替换为实际的动画路径
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "animations/entity/centipede_head.animation.json");
    }
}
