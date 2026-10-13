package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.animation.AnimationProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xiaoshi2022.corpseorigin.client.render.layer.NativeCustomOrgans;

/** Only native organ adapters unwrap; all existing GeckoLib animatables retain their original actor. */
@Mixin(value = AnimationProcessor.class, remap = false)
public abstract class NativeOrganMolangMixin {
    @ModifyArg(method = "extractControllerStates", at = @At(value = "INVOKE",
            target = "Lcom/geckolib/loading/math/MolangQueries$Actor;<init>(Ljava/lang/Object;Lcom/geckolib/renderer/base/GeoRenderState;Lcom/geckolib/animation/AnimationController;DFLnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)V"), index = 0)
    private static Object corpseorigin$hostQueries(Object animatable) {
        return NativeCustomOrgans.queryHost(animatable);
    }
}