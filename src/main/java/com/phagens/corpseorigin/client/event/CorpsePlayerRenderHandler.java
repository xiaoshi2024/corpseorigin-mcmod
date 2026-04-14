package com.phagens.corpseorigin.client.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.Renderer.layer.ExoskeletonRenderLayer;
import com.phagens.corpseorigin.client.Renderer.layer.MultiEyeRenderLayer;
import com.phagens.corpseorigin.client.Renderer.layer.TailRenderLayer;
import com.phagens.corpseorigin.client.Renderer.layer.WingRenderLayer;
import com.phagens.corpseorigin.client.model.*;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 尸兄玩家渲染处理器
 * 负责在玩家成为尸兄时添加外骨骼渲染层
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class CorpsePlayerRenderHandler {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        CorpseOrigin.LOGGER.info("Adding exoskeleton layer to player renderer");

        EntityModelSet modelSet = event.getContext().getModelSet();

        // 为默认玩家渲染器添加外骨骼层
        PlayerRenderer defaultRenderer = event.getSkin(PlayerSkin.Model.WIDE);
        addExoskeletonLayer(defaultRenderer, modelSet);

        // 为纤细模型玩家渲染器添加外骨骼层
        PlayerRenderer slimRenderer = event.getSkin(PlayerSkin.Model.SLIM);
        addExoskeletonLayer(slimRenderer, modelSet);
    }

    private static void addExoskeletonLayer(PlayerRenderer renderer, EntityModelSet modelSet) {
        if (renderer == null) return;

        // 添加外骨骼渲染层
        ExoskeletonModel exoskeletonModel = new ExoskeletonModel(
                modelSet.bakeLayer(CorpsemoldelRegister.EXOSKELETON_LAYER)
        );
        renderer.addLayer(new ExoskeletonRenderLayer(renderer, exoskeletonModel));
        
        // 添加羽翼渲染层
        WingModel wingModel = new WingModel(
                modelSet.bakeLayer(CorpsemoldelRegister.WING_LAYER)
        );
        renderer.addLayer(new WingRenderLayer(renderer, wingModel));
        
        // 添加鱼尾渲染层
        TailModel tailModel = new TailModel(
                modelSet.bakeLayer(CorpsemoldelRegister.TAIL_LAYER)
        );
        renderer.addLayer(new TailRenderLayer(renderer, tailModel));

        // 添加多眼渲染层
        MultiEyeModel multiEyeModel = new MultiEyeModel(
                modelSet.bakeLayer(CorpsemoldelRegister.MULTI_EYE_LAYER)
        );
        renderer.addLayer(new MultiEyeRenderLayer(renderer, multiEyeModel));
    }
}
