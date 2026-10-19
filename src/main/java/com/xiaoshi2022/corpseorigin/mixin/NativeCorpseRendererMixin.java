package com.xiaoshi2022.corpseorigin.mixin;

import com.xiaoshi2022.corpseorigin.client.render.layer.NativeCorpseLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 感染渲染层注入：为所有 LivingEntityRenderer（原版 + 模组）附加 NativeCorpseLayer。
 * <p>
 * 1.20.1 Forge：只需注入构造函数，不需要像 Fabric 26.2 那样还注入 extractRenderState
 * （1.20.1 没有 extractRenderState 方法，感染状态直接在 NativeCorpseLayer.render()
 * 里从 entity 读取 NativeCorpseState.infected(entity)）。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class NativeCorpseRendererMixin {

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void corpseorigin$customBody(EntityModel<?> model, com.mojang.blaze3d.vertex.PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a,
            LivingEntity entity, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack originalPose,
            net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight) {
        if (!com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.replacesBody(entity))
            model.renderToBuffer(pose, buffer, light, overlay, r, g, b, a);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void corpseorigin$attachNativeLayer(CallbackInfo ci) {
        LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
        // LivingEntityRenderer.addLayer 签名：addLayer(RenderLayer<T, M> layer)
        // NativeCorpseLayer 继承 RenderLayer<LivingEntity, EntityModel<LivingEntity>>
        renderer.addLayer(new NativeCorpseLayer((RenderLayerParent) renderer));
        renderer.addLayer(new com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer((RenderLayerParent) renderer));
    }
}
