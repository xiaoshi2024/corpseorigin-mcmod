package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.render.layer.NativeCorpseLayer;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

@Mixin(LivingEntityRenderer.class)
public abstract class NativeCorpseRendererMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void corpseorigin$attachNativeLayer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, net.minecraft.client.model.EntityModel model, float radius, CallbackInfo ci) {
        xiaoshi2022.corpseorigin.client.render.layer.NativeCustomOrgans.init(context);
        ((LivingEntityRendererMixin) (Object) this).callAddLayer(new NativeCorpseLayer((RenderLayerParent) this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void corpseorigin$nativeInfection(LivingEntity entity, LivingEntityRenderState state,
                                             float partialTick, CallbackInfo ci) {
        // Always write false too: render-state reuse must not infect the next healthy entity visually.
        state.addGeckolibData(NativeCorpseLayer.INFECTED, NativeCorpseState.infected(entity));
        xiaoshi2022.corpseorigin.client.render.layer.NativeCustomOrgans.extract(entity, state, partialTick);
    }
}
