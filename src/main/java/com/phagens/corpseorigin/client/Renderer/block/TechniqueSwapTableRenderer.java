package com.phagens.corpseorigin.client.Renderer.block;

import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import com.phagens.corpseorigin.client.Models.block.TechniqueSwapTableModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TechniqueSwapTableRenderer extends GeoBlockRenderer<TechniqueSwapTableEntity> {
    public TechniqueSwapTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new TechniqueSwapTableModel());
    }
}
