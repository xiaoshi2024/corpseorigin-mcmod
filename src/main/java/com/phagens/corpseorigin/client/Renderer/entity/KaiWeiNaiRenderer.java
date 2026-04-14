package com.phagens.corpseorigin.client.Renderer.entity;

import com.phagens.corpseorigin.client.Models.entity.KaiWeiNaiModel;
import com.phagens.corpseorigin.entity.npc.KaiWeiNaiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 开胃奶NPC渲染器
 * 使用GeckoLib模型渲染
 */
public class KaiWeiNaiRenderer extends GeoEntityRenderer<KaiWeiNaiEntity> {

    public KaiWeiNaiRenderer(EntityRendererProvider.Context context) {
        super(context, new KaiWeiNaiModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(KaiWeiNaiEntity entity) {
        // 使用模型中定义的纹理
        return super.getTextureLocation(entity);
    }

}
