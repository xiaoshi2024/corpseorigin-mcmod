package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.renderer.armor.AntennaArmFirstPerson;

/**
 * 首人称手臂：穿了天线宝宝胸甲时，用盔甲自己的手臂模型替掉原版那条光手。
 * <p>
 * 注入点选 {@code renderPlayerArm} —— 这正是 NeoForge 那边 {@code RenderArmEvent} 的落点
 * （Fabric 没有对应事件）。原版只在"手上没东西、没隐身、没开镜"时才调用它，
 * 所以"空手才替换"这个条件天然成立，不用自己判。
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$injectChest(net.minecraft.client.player.AbstractClientPlayer player,
            float partialTick, float pitch, net.minecraft.world.InteractionHand hand, float swing,
            net.minecraft.world.item.ItemStack stack, float equip, PoseStack poses,
            SubmitNodeCollector collector, int light, CallbackInfo ci) {
        HumanoidArm arm = hand == net.minecraft.world.InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        if (!xiaoshi2022.corpseorigin.client.render.SagentInjectionPose.active(player, arm, stack)) return;
        float t = xiaoshi2022.corpseorigin.client.render.SagentInjectionPose.approach(player, partialTick);
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poses.pushPose();
        // Move from the held side to the left chest, below the eye line, needle facing inward.
        poses.translate(side * .48f * (1 - t) - .12f * t, -.40f - .12f * t, -.72f + .35f * t);
        poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
        ((ItemInHandRenderer)(Object)this).renderItem(player, stack,
                net.minecraft.world.item.ItemDisplayContext.NONE, poses, collector, light);
        poses.popPose();
        ci.cancel();
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$renderAntennaArm(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                               float equipProgress, float swingProgress, HumanoidArm arm,
                                               CallbackInfo ci) {
        if (AntennaArmFirstPerson.render(poseStack, collector, packedLight, equipProgress, swingProgress, arm)) {
            ci.cancel();
        }
    }
}
