package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual shrink only: health, physical size and other scale modifiers remain unchanged. */
@Mixin(LivingEntityRenderer.class)
public abstract class GourdCaptureRenderMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("RETURN"))
    private void corpseorigin$captureScale(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo ci){
        state.scale*=Math.clamp(entity.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.SCALE),.06f,1f);
    }
}
