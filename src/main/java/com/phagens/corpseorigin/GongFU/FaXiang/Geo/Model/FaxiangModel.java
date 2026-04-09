package com.phagens.corpseorigin.GongFU.FaXiang.Geo.Model;

import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FaxiangModel extends GeoModel<FaxiangEntity> {
    @Override
    public ResourceLocation getModelResource(FaxiangEntity animatable) {
        return animatable.getModelResource();
    }

    @Override
    public ResourceLocation getTextureResource(FaxiangEntity animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(FaxiangEntity animatable) {
        return animatable.getAnimationResource();
    }
}
