package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import java.util.Map;

/** Vanilla routes every AvatarRenderState to a player renderer, including clone snapshots. */
@Mixin(EntityRenderDispatcher.class)
public abstract class CloneRenderDispatcherMixin {
    @Shadow private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Inject(method="getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
            at=@At("HEAD"),cancellable=true)
    private void corpseorigin$cloneRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        if(state.entityType==ModEntities.CLONE_AVATAR) {
            var renderer=renderers.get(ModEntities.CLONE_AVATAR);
            if(renderer!=null)cir.setReturnValue(renderer);
        }
    }
}
