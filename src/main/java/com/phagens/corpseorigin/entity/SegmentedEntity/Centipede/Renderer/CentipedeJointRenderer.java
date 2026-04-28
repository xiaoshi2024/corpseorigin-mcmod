package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Renderer;

import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeJoint;
import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Model.CentipedeJointModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CentipedeJointRenderer extends GeoEntityRenderer<CentipedeJoint> {
    public CentipedeJointRenderer(EntityRendererProvider.Context context) {
        super(context, new CentipedeJointModel());
        this.withScale(3.0f);
        // 修复：调整模型渲染偏移，让模型显示在碰撞箱中心
        this.shadowRadius = 1.0f;
    }
}
