package com.phagens.corpseorigin.entity.runEntityBeam.Model;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.runEntityBeam.Entity.LaserBeamEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// LaserBeamModel.java
public class LaserBeamModel extends GeoModel<LaserBeamEntity> {
    @Override
    public ResourceLocation getModelResource(LaserBeamEntity animatable) {
        // 指向geo.json 文件路径
        return  ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/laser_beam.geo.json ");
    }

    @Override
    public ResourceLocation getTextureResource(LaserBeamEntity animatable) {
        // 指向纹理文件路径
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/laser_beam.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LaserBeamEntity animatable) {
        // 指向动画文件路径
        return null;
    }
}

