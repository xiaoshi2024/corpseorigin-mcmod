package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Renderer;

import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeJoint;
import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Model.CentipedeJointModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CentipedeJointRenderer extends GeoEntityRenderer<CentipedeJoint> {
    public CentipedeJointRenderer(EntityRendererProvider.Context context) {
        super(context, new CentipedeJointModel());

    }
}
