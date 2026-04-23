package com.phagens.corpseorigin.compat.curios;

import com.phagens.corpseorigin.client.Renderer.item.JuQueRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * JuQue Curios 渲染器 - 使用反射机制实现软依赖
 * 
 * 【功能说明】
 * 1. 为 JuQue 物品提供 Curios 渲染支持
 * 2. 使用反射机制调用 Curios API
 * 3. 避免编译时硬依赖 Curios 模组
 */
public class JuQueCuriosRenderer {
    
    private final JuQueRenderer juQueRenderer;
    
    public JuQueCuriosRenderer() {
        this.juQueRenderer = new JuQueRenderer();
    }
    
    /**
     * 渲染 Curios 饰品
     */
    public void render(ItemStack stack, Object slotContext, Object poseStack, 
                      Object renderLayerParent, Object buffer, int light, 
                      float limbSwing, float limbSwingAmount, float partialTicks, 
                      float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            // 调用 JuQueRenderer 的 Curios 渲染方法
            juQueRenderer.renderCurios(stack, slotContext, 
                (com.mojang.blaze3d.vertex.PoseStack) poseStack, 
                (RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) renderLayerParent, 
                (MultiBufferSource) buffer, light, limbSwing, limbSwingAmount, 
                partialTicks, ageInTicks, netHeadYaw, headPitch);
        } catch (Exception e) {
            // 忽略反射错误
        }
    }
    
    /**
     * 获取 JuQue 渲染器
     */
    public JuQueRenderer getJuQueRenderer() {
        return juQueRenderer;
    }
}