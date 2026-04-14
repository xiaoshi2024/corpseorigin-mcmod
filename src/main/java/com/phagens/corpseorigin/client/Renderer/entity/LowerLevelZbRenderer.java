package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phagens.corpseorigin.client.Models.entity.LowerLevelZbModel;
import com.phagens.corpseorigin.client.skin.ZbSkinState;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.UUID;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class LowerLevelZbRenderer extends GeoEntityRenderer<LowerLevelZbEntity> {
    // 尸化骨骼覆盖纹理
    private static final ResourceLocation SKELETON_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/lower_level_zb_render.png");
    private static final ResourceLocation CRACKED_SKELETON_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/lower_level_zb_rendering.png");

    public LowerLevelZbRenderer(EntityRendererProvider.Context context) {
        super(context, new LowerLevelZbModel());
        this.addRenderLayer(new PlayerSkinLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(LowerLevelZbEntity animatable) {
        // 这个方法不会被调用，因为我们使用渲染层
        // 但需要返回一个默认值
        return DefaultPlayerSkin.getDefaultTexture();
    }

    private class PlayerSkinLayer extends GeoRenderLayer<LowerLevelZbEntity> {
        public PlayerSkinLayer(GeoEntityRenderer<LowerLevelZbEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, LowerLevelZbEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {

            // 确定使用哪个皮肤纹理
            ResourceLocation skinTexture = determineSkinTexture(animatable);

            // 渲染玩家皮肤作为基础 - 完全不透明
            RenderType skinRenderType = RenderType.entityTranslucent(skinTexture);
            VertexConsumer skinConsumer = bufferSource.getBuffer(skinRenderType);

            // 使用 reRender 方法（带颜色参数）
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    skinRenderType, skinConsumer, partialTick, packedLight, packedOverlay,
                    0xFFFFFFFF); // 白色，完全不透明

            // 叠加尸化骨骼纹理 - 80% 透明度
            ResourceLocation overlayTexture = animatable.getVariant() == com.phagens.corpseorigin.entity.LowerLevelZbEntity.Variant.CRACKED ?
                    CRACKED_SKELETON_OVERLAY : SKELETON_OVERLAY;
            RenderType skeletonRenderType = RenderType.entityTranslucent(overlayTexture);
            VertexConsumer skeletonConsumer = bufferSource.getBuffer(skeletonRenderType);

            // 0xCCFFFFFF = 白色，80%透明度
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    skeletonRenderType, skeletonConsumer, partialTick, packedLight, packedOverlay,
                    0xCCFFFFFF);
        }

        private ResourceLocation determineSkinTexture(LowerLevelZbEntity entity) {
            // 如果皮肤已加载，使用自定义皮肤
            if (entity.getSkinState() == ZbSkinState.LOADED) {
                ResourceLocation customSkin = entity.getSkinTexture();
                if (customSkin != null) {
                    return customSkin;
                }
            }

            // 如果有玩家名，尝试基于UUID生成默认皮肤
            String playerName = entity.getPlayerSkinName();
            if (playerName != null && !playerName.isEmpty()) {
                // 根据玩家名生成一个稳定的UUID，这样同一玩家对应的尸兄会有相同的默认皮肤
                UUID fakeUuid = UUID.nameUUIDFromBytes(playerName.getBytes());
                return DefaultPlayerSkin.get(fakeUuid).texture();
            }

            // 最后回退到默认Steve皮肤
            return DefaultPlayerSkin.getDefaultTexture();
        }
    }
}