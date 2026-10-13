package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.render.layer.NativeCorpseLayer;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

/** The dragon does not use LivingEntityRenderer, so append inside its own posed model space. */
@Mixin(EnderDragonRenderer.class)
public abstract class NativeCorpseDragonRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void corpseorigin$context(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, CallbackInfo ci) {
        xiaoshi2022.corpseorigin.client.render.layer.NativeCustomOrgans.init(context);
    }
    @Shadow @Final private EnderDragonModel model;
    @Unique private final NativeCorpseLayer.Organs corpseorigin$organs = new NativeCorpseLayer.Organs();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;F)V", at = @At("RETURN"))
    private void corpseorigin$infection(EnderDragon entity, EnderDragonRenderState state, float partial, CallbackInfo ci) {
        state.addGeckolibData(NativeCorpseLayer.INFECTED, NativeCorpseState.infected(entity) && !entity.isInvisible());
        xiaoshi2022.corpseorigin.client.render.layer.NativeCustomOrgans.extract(entity, state, partial);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;pack(FZ)I"))
    private void corpseorigin$organs(EnderDragonRenderState state, PoseStack poses, SubmitNodeCollector collector,
                                     CameraRenderState camera, CallbackInfo ci) {
        if (state.deathTime > 0 || !Boolean.TRUE.equals(state.getGeckolibData(NativeCorpseLayer.INFECTED))) return;
        model.setupAnim(state);
        corpseorigin$organs.submit(model, state, poses, collector, state.lightCoords);
    }
}
