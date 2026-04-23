package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Renderer;

import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeHead;
import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Model.CentipedeHeadModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CentipedeHeadRenderer extends GeoEntityRenderer<CentipedeHead> {
    public CentipedeHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new CentipedeHeadModel());
    }
}
