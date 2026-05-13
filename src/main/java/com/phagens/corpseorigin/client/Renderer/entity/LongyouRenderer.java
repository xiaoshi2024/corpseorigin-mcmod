package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.phagens.corpseorigin.client.Models.entity.LongyouModel;
import com.phagens.corpseorigin.entity.LongyouEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class LongyouRenderer extends GeoEntityRenderer<LongyouEntity> {

    public LongyouRenderer(EntityRendererProvider.Context context) {
        super(context, new LongyouModel());

        // 添加手持物品渲染层
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, LongyouEntity entity) {
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
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, LongyouEntity entity) {
                // 设置第三人称手持渲染模式
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, LongyouEntity entity,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                // 微调物品位置和旋转（根据你的模型调整数值）
                poseStack.translate(0, 0.1, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                super.renderStackForBone(poseStack, bone, stack, entity, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(LongyouEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("corpseorigin", "textures/entity/longyou.png");
    }
}