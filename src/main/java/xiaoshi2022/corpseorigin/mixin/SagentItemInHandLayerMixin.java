package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.render.SagentInjectionPose;

@Mixin(ItemInHandLayer.class)
public abstract class SagentItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$needleAtHeart(ArmedEntityRenderState state, ItemStackRenderState itemState,
            ItemStack stack, HumanoidArm arm, PoseStack poses, SubmitNodeCollector collector,
            int light, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        var level = Minecraft.getInstance().level;
        var actor = level == null ? null : level.getEntity(avatar.id);
        if (actor == null || !SagentInjectionPose.active(actor, arm, stack)) return;
        if (!(((RenderLayer<?, ?>)(Object)this).getParentModel() instanceof HumanoidModel<?> model)) return;
        float t = SagentInjectionPose.approach(actor, state.ageInTicks - (float)Math.floor(state.ageInTicks));
        poses.pushPose();
        model.body.translateAndRotate(poses);
        // Player model coordinates: +X is the left chest, -Z is in front of the torso.
        poses.translate((arm == HumanoidArm.RIGHT ? -.32f : .32f) * (1 - t) + .12f * t,
                .30f, -.85f + .25f * t);
        // Needle points along model -Y; rotate it toward the chest (+Z).
        poses.mulPose(Axis.XP.rotationDegrees(-90));
        itemState.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poses.popPose();
        ci.cancel();
    }
}
