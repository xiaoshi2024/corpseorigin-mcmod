package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        AvatarRenderer self = (AvatarRenderer) (Object) this;

        try {
            var modelSet = Minecraft.getInstance().getEntityModels();
            var exoskeletonModel = new ExoskeletonModel(modelSet.bakeLayer(ModModelLayers.EXOSKELETON));

            var layer = new ExoskeletonRenderLayer(self, exoskeletonModel);

            ((LivingEntityRendererMixin) self).callAddLayer(layer);

            // ✅ 保存引用
            CorpsePlayerRenderHandler.LAYER_MAP.put(self, layer);

            CorpseOrigin.LOGGER.info("✅ 外骨骼渲染层已添加到 AvatarRenderer: {}", self);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 添加外骨骼渲染层失败: {}", e.getMessage(), e);
        }
    }
}