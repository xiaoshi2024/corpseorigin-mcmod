package com.phagens.corpseorigin.client.Renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.model.CorpseGibModel;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * 尸体残肢渲染器
 *
 * 【功能说明】
 * 完全参照 Mob-Dismemberment 1.12.2 的 RenderGib 设计
 * 从父实体获取纹理并渲染对应的残肢部件
 *
 * 【参照 RenderGib.doRender】
 * 1. 禁用面剔除
 * 2. 绑定父实体纹理
 * 3. 启用混合和透明度
 * 4. 根据落地时间调整透明度
 * 5. 应用位置、旋转变换
 * 6. 根据残肢类型调整 Y 轴偏移
 * 7. 渲染模型
 */
@OnlyIn(Dist.CLIENT)
public class CorpseGibRenderer extends EntityRenderer<CorpseGibEntity> {

    private final CorpseGibModel model;

    // 纹理缓存
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    // 默认纹理
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public CorpseGibRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
        this.model = new CorpseGibModel();
    }

    @Override
    public void render(CorpseGibEntity gib, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        // 参照 RenderGib.doRender

        // 保存矩阵状态
        poseStack.pushPose();

        // 获取纹理
        ResourceLocation texture = getTextureLocation(gib);

        // 计算透明度 (落地后逐渐消失) - 使用与实体相同的常量
        float alpha = 1.0F;
        int groundTime = gib.getGroundTime();
        if (groundTime >= CorpseGibEntity.GROUND_TIME_BEFORE_FADE) {
            alpha = Mth.clamp(1.0F - (groundTime - CorpseGibEntity.GROUND_TIME_BEFORE_FADE + partialTicks) / CorpseGibEntity.GROUND_TIME_FADE_DURATION, 0F, 1F);
        }

        // 设置渲染类型 (支持透明度)
        RenderType renderType = RenderType.entityTranslucent(texture);

        // 应用变换
        // GlStateManager.translate(par2, par4, par6); - 已在父类中处理

        // 根据残肢类型调整 Y 轴偏移
        // 参照: GlStateManager.translate(0.0F, gib.type == 0 ? 4F / 16F : ...
        float yOffset = getYOffsetByType(gib.getGibType(), gib.isSkeleton());
        poseStack.translate(0.0F, yOffset, 0.0F);

        // 应用旋转
        // 参照: GlStateManager.rotate(EntityHelper.interpolateRotation(gib.prevRotationYaw, gib.rotationYaw, par9), 0.0F, 1.0F, 0.0F);
        float yaw = Mth.lerp(partialTicks, gib.yRotO, gib.getYRot());
        float pitch = Mth.lerp(partialTicks, gib.xRotO, gib.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // 参照: GlStateManager.translate(0.0F, 24F / 16F - gib.height * 0.5F, 0.0F);
        poseStack.translate(0.0F, 1.5F - gib.getBbHeight() * 0.5F, 0.0F);

        // 参照: GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        // 设置模型参数
        model.setRenderParams(gib.getGibType(), !gib.isSkeleton());

        // 渲染模型
        // 参照: modelGib.render(gib, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        int color = (int)(alpha * 255) << 24 | 0xFFFFFF;
        model.renderToBuffer(poseStack, buffer.getBuffer(renderType), packedLight,
                OverlayTexture.NO_OVERLAY, color);

        // 恢复矩阵状态
        poseStack.popPose();

        super.render(gib, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * 根据残肢类型获取 Y 轴偏移
     * 参照 RenderGib.doRender 中的偏移计算
     */
    private float getYOffsetByType(int type, boolean isSkeleton) {
        return switch (type) {
            case CorpseGibEntity.GIB_TYPE_HEAD -> 4F / 16F;
            case CorpseGibEntity.GIB_TYPE_LEFT_ARM, CorpseGibEntity.GIB_TYPE_RIGHT_ARM -> {
                if (isSkeleton) {
                    yield 1F / 16F;
                } else {
                    yield 2F / 16F;
                }
            }
            default -> 2F / 16F;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseGibEntity gib) {
        String parentType = gib.getParentType();

        // 从缓存获取
        if (TEXTURE_CACHE.containsKey(parentType)) {
            return TEXTURE_CACHE.get(parentType);
        }

        // 参照 RenderGib.getEntityTexture
        // 尝试从父实体获取纹理
        try {
            LivingEntity parent = gib.getParent();
            if (parent != null) {
                ResourceLocation texture = getEntityTexture(parent);
                if (texture != null) {
                    TEXTURE_CACHE.put(parentType, texture);
                    return texture;
                }
            }

            // 尝试创建临时实体获取纹理
            EntityType<?> type = EntityType.byString(parentType).orElse(null);
            if (type != null) {
                LivingEntity tempEntity = (LivingEntity) type.create(Minecraft.getInstance().level);
                if (tempEntity != null) {
                    ResourceLocation texture = getEntityTexture(tempEntity);
                    if (texture != null) {
                        TEXTURE_CACHE.put(parentType, texture);
                        tempEntity.discard();
                        return texture;
                    }
                    tempEntity.discard();
                }
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("获取残肢纹理失败: {}", parentType);
        }

        // 默认纹理
        return DEFAULT_TEXTURE;
    }

    /**
     * 获取实体的纹理
     * 参照 Mob-Dismemberment 的方式
     */
    private ResourceLocation getEntityTexture(LivingEntity entity) {
        try {
            EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                return livingRenderer.getTextureLocation(entity);
            }
        } catch (Exception e) {
            // 失败返回 null
        }
        return null;
    }
}
