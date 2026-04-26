package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Model;

import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeJoint;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CentipedeJointModel extends GeoModel<CentipedeJoint> {
    
    @Override
    public ResourceLocation getModelResource(CentipedeJoint animatable) {
        // TODO: 替换为实际的模型路径
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "geo/entity/centipede_joint.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CentipedeJoint animatable) {
        // TODO: 替换为实际的纹理路径
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/centipede_joint.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CentipedeJoint animatable) {
        // TODO: 替换为实际的动画路径
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "animations/entity/centipede_joint.animation.json");
    }
}
