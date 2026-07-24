// 文件路径: src/main/java/com/phagens/corpseorigin/client/Models/entity/MaotuProjectileModel.java
package com.phagens.corpseorigin.client.Models.entity;

import com.phagens.corpseorigin.CorpseOrigin;

import com.phagens.corpseorigin.entity.MaotuProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MaotuProjectileModel extends GeoModel<MaotuProjectileEntity> {

    @Override
    public ResourceLocation getModelResource(MaotuProjectileEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/entity/maotu_projectile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MaotuProjectileEntity object) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/entity/maotu_projectile.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MaotuProjectileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "animations/entity/maotu_projectile.animation.json");
    }
}
