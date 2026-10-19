package com.xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.corpseorigin.client.render.NativeCorpseOrgans;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Locale;

/**
 * GeckoLib 模型的感染叠加层（骨骼级）。
 * <p>
 * 与 {@link NativeCorpseLayer} 的区别：这里宿主本身是 GeckoLib 模型，
 * 直接在 {@code renderForBone} 钩子里按 GeoBone 叠加：
 * <ul>
 *   <li>静脉：把该骨骼的 GeoCube 用静脉贴图再渲染一遍（微量外扩）</li>
 *   <li>肋骨：挂到 body/torso/chest 骨骼</li>
 *   <li>眼睛：挂到 head/neck 骨骼</li>
 * </ul>
 * 挂载方式：
 * <pre>{@code
 * NativeCorpseGeoLayer.install(myGeoRenderer);
 * }</pre>
 */
public class NativeCorpseGeoLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {

    public NativeCorpseGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    /** 把本层挂到任意 GeckoLib 渲染器上。 */
    public static <T extends LivingEntity & GeoAnimatable> void install(GeoRenderer<T> renderer) {
        renderer.getRenderLayers().add(new NativeCorpseGeoLayer<>(renderer));
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isInvisible() || !NativeCorpseState.transformed(animatable)) return;

        // 1) 静脉：该骨骼所有 cube 用静脉贴图重画一遍
        VertexConsumer veins = bufferSource.getBuffer(RenderType.entityCutoutNoCullZOffset(NativeCorpseOrgans.VEINS));
        poseStack.pushPose();

        getRenderer().renderCubesOfBone(poseStack, bone, veins, packedLight, packedOverlay,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        int mask = NativeCorpseState.renderAppearance(animatable);
        if (mask == 0) return;

        String name = bone.getName().toLowerCase(Locale.ROOT);

        // 2) 肋骨：body 骨骼处
        if ((mask & NativeCorpseState.RIBS) != 0 && isBodyBone(name)) {
            NativeCorpseOrgans.ribs().render(poseStack,
                    bufferSource.getBuffer(RenderType.entityCutoutNoCull(NativeCorpseOrgans.TISSUE)),
                    packedLight, OverlayTexture.NO_OVERLAY);
        }

        // 3) 眼睛：head 骨骼处
        if ((mask & NativeCorpseState.EYE) != 0 && isHeadBone(name)) {
            poseStack.pushPose();
            poseStack.scale(2.0F, 2.0F, 2.0F);
            NativeCorpseOrgans.eye().render(poseStack,
                    bufferSource.getBuffer(RenderType.entityTranslucent(NativeCorpseOrgans.EYE)),
                    packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static boolean isBodyBone(String name) {
        return name.equals("body") || name.equals("torso") || name.equals("chest")
                || name.equals("upper_body") || name.equals("waist") || name.equals("main");
    }

    private static boolean isHeadBone(String name) {
        if (name.contains("hat") || name.contains("brim")) return false;
        return name.equals("head") || name.equals("head_main") || name.equals("center_head")
                || name.equals("middle_head") || name.equals("neck");
    }
}
