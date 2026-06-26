package com.phagens.corpseorigin.client.Renderer.item;

import com.phagens.corpseorigin.Item.Swrod.JuQue;
import com.phagens.corpseorigin.client.Models.item.JuQueModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class JuQueRenderer extends GeoItemRenderer<JuQue> {
    public JuQueRenderer() {
        super(new JuQueModel());
    }
    
    // ================= Curios 渲染支持 =================
    
    /**
     * Curios 渲染方法 - 背部饰品
     * 使用反射机制实现软依赖
     */
    public void renderCurios(ItemStack stack, Object slotContext, PoseStack poseStack, 
                            RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> renderLayerParent, 
                            MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, 
                            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (renderLayerParent.getModel() instanceof HumanoidModel) {
            HumanoidModel<?> model = (HumanoidModel<?>) renderLayerParent.getModel();

            // 将模型移动到背部位置
            model.body.translateAndRotate(poseStack);

            // 调整位置和旋转，使其看起来背在背上（正常背着的姿势）
            poseStack.scale(1.0F, 1.0F, 1.0F);
            poseStack.translate(-0.3F, -0.6F, -0.3F); // 移动到背部中央
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0.0F)); // 翻转方向，使剑柄朝上
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-45.0F)); // 旋转到垂直位置
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F)); // 保持水平方向

            // 渲染物品
            this.renderByItem(stack, net.minecraft.world.item.ItemDisplayContext.FIXED,
                    poseStack, buffer, light, 0xF000F0);
        }
    }
}
