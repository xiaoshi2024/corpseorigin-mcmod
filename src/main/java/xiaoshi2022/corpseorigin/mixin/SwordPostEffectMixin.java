package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A short extra pass; never replaces the player's existing spectator/resource-pack effect. */
@Mixin(GameRenderer.class)
public abstract class SwordPostEffectMixin {
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void corpseorigin$swordPost(DeltaTracker delta,CallbackInfo ci){
        int stage=xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.postStage();
        if(stage==0)return;
        var chain=Minecraft.getInstance().getShaderManager().getPostChain(
                xiaoshi2022.corpseorigin.CorpseOrigin.id("sword_impact_"+stage),java.util.Set.of(PostChain.MAIN_TARGET_ID));
        if(chain!=null)chain.process(mainRenderTarget,resourcePool);
    }
}
