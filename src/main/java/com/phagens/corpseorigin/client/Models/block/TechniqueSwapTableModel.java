package com.phagens.corpseorigin.client.Models.block;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechniqueSwapTableModel extends GeoModel<TechniqueSwapTableEntity> {
    
    @Override
    public ResourceLocation getModelResource(TechniqueSwapTableEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "geo/block/technique_swap_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TechniqueSwapTableEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/block/technique_swap_table.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TechniqueSwapTableEntity animatable) {
        return null; // 静态方块不需要动画
    }
}
