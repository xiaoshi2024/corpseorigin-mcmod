package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.Models.entity.LowerLevelZbModel;
import com.phagens.corpseorigin.client.mca.McaSkinHelper;
import com.phagens.corpseorigin.entity.mca.McaZombieEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

@OnlyIn(Dist.CLIENT)
public class McaZombieRenderer extends GeoEntityRenderer<McaZombieEntity> {
    private static final ResourceLocation SKELETON_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/lower_level_zb_render.png");
    private static final ResourceLocation CRACKED_SKELETON_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/lower_level_zb_rendering.png");

    public McaZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new McaZombieModel());
        this.addRenderLayer(new McaSkinLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(McaZombieEntity animatable) {
        return SKELETON_OVERLAY;
    }

    private class McaSkinLayer extends GeoRenderLayer<McaZombieEntity> {
        public McaSkinLayer(GeoEntityRenderer<McaZombieEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, McaZombieEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {

            if (McaSkinHelper.isMcaAvailable()) {
                Optional<ResourceLocation> mcaSkin = getMcaSkinTexture(animatable);
                if (mcaSkin.isPresent()) {
                    RenderType skinRenderType = RenderType.entityTranslucent(mcaSkin.get());
                    VertexConsumer skinConsumer = bufferSource.getBuffer(skinRenderType);

                    int skinColor = getMcaSkinColor(animatable);
                    this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                            skinRenderType, skinConsumer, partialTick, packedLight, packedOverlay,
                            skinColor);
                }
            }

            ResourceLocation overlayTexture = animatable.getVariant() == com.phagens.corpseorigin.entity.LowerLevelZbEntity.Variant.CRACKED ?
                    CRACKED_SKELETON_OVERLAY : SKELETON_OVERLAY;
            RenderType skeletonRenderType = RenderType.entityTranslucent(overlayTexture);
            VertexConsumer skeletonConsumer = bufferSource.getBuffer(skeletonRenderType);

            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    skeletonRenderType, skeletonConsumer, partialTick, packedLight, packedOverlay,
                    0xCCFFFFFF);
        }

        private Optional<ResourceLocation> getMcaSkinTexture(McaZombieEntity entity) {
            // 优先使用客户端缓存的数据
            if (entity.hasClientSkinData()) {
                String gender = entity.getClientSkinGender();
                int skinIndex = entity.getClientSkinIndex();
                String skinPath = "skins/skin/" + gender + "/" + skinIndex + ".png";
                return Optional.of(ResourceLocation.parse("mca:" + skinPath));
            }

            // 降级：尝试使用 mcaVillagerLike
            Object mcaVillagerLike = entity.getMcaVillagerLike();
            if (mcaVillagerLike == null) {
                return Optional.empty();
            }

            return McaSkinHelper.getMcaSkinReflective(mcaVillagerLike);
        }

        private int getMcaSkinColor(McaZombieEntity entity) {
            try {
                Object mcaVillagerLike = entity.getMcaVillagerLike();
                if (mcaVillagerLike == null) {
                    return 0xFFFFFFFF;
                }

                // 使用反射而不是强制转换
                Optional<int[]> color = McaSkinHelper.getMcaSkinColorReflective(mcaVillagerLike);
                if (color.isPresent()) {
                    int[] rgb = color.get();
                    return (0xFF << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
                }
            } catch (Exception e) {
                CorpseOrigin.LOGGER.debug("获取MCA皮肤颜色失败: {}", e.getMessage());
            }
            return 0xFFFFFFFF;
        }
    }

    public static class McaZombieModel extends GeoModel<McaZombieEntity> {
        @Override
        public net.minecraft.resources.ResourceLocation getModelResource(McaZombieEntity object) {
            switch (object.getVariant()) {
                case CRACKED:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/lower_level_zb_rendering.geo.json");
                case WINGS:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/hybrid/lower_level_zb_wings.geo.json");
                case WINGS_CRACKED:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/hybrid/lower_level_zb_rendering_wings.geo.json");
                default:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/lower_level_zb.geo.json");
            }
        }

        @Override
        public net.minecraft.resources.ResourceLocation getTextureResource(McaZombieEntity object) {
            switch (object.getVariant()) {
                case CRACKED:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/lower_level_zb_rendering.png");
                case WINGS:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/hybrid/lower_level_zb_wings.png");
                case WINGS_CRACKED:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/hybrid/lower_level_zb_rendering_wings.png");
                default:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/lower_level_zb_render.png");
            }
        }

        @Override
        public net.minecraft.resources.ResourceLocation getAnimationResource(McaZombieEntity animatable) {
            switch (animatable.getVariant()) {
                case CRACKED:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "animations/entity/lower_level_zb_rendering.animation.json");
                case WINGS:
                case WINGS_CRACKED:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "animations/entity/hybrid/lower_level_zb_wings.animation.json");
                default:
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "animations/entity/lower_level_zb.animation.json");
            }
        }
    }
}
