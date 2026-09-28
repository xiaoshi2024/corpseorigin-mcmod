package xiaoshi2022.corpseorigin.mixin;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderer.class)
public abstract class SwordHitStopMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",at=@At("TAIL"))
    private void corpseorigin$swordHitStop(Entity entity,EntityRenderState state,float partial,CallbackInfo ci){
        xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer.freeze(entity.getId(),state);
    }
}
