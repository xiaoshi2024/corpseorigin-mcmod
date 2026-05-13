package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.phagens.corpseorigin.client.Models.entity.LowerLevelZbModel;
import com.phagens.corpseorigin.client.skin.ZbSkinState;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import javax.annotation.Nullable;
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

        // 添加手持物品渲染层
        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, LowerLevelZbEntity entity) {
                // 绑定右手骨骼
                if ("rightItem".equals(bone.getName())) {
                    return entity.getMainHandItem();
                }
                // 绑定左手骨骼（如果模型有 leftItem）
                if ("leftItem".equals(bone.getName())) {
                    return entity.getOffhandItem();
                }
                return null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, LowerLevelZbEntity entity) {
                // 设置第三人称手持渲染模式
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, LowerLevelZbEntity entity,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                // 微调物品位置和旋转（根据你的模型调整数值）
                poseStack.translate(0, 0.1, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                super.renderStackForBone(poseStack, bone, stack, entity, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(LowerLevelZbEntity animatable) {
        // 这个方法可能会被调用，所以返回一个有效的默认值
        return SKELETON_OVERLAY;
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
            if (skinTexture != null) {
                RenderType skinRenderType = RenderType.entityTranslucent(skinTexture);
                VertexConsumer skinConsumer = bufferSource.getBuffer(skinRenderType);

                // 使用 reRender 方法（带颜色参数）
                this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                        skinRenderType, skinConsumer, partialTick, packedLight, packedOverlay,
                        0xFFFFFFFF); // 白色，完全不透明
            }

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