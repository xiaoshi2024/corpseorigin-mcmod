package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.client.render.QiAuraRenderer;

@Mixin(FogRenderer.class)
public class QiFogMixin {
    @Inject(method="setupFog",at=@At("RETURN"))
    private void corpseorigin$qiFog(Camera camera,int distance,DeltaTracker delta,float darken,ClientLevel level,CallbackInfoReturnable<FogData> cir){
        QiAuraRenderer.fog(camera,cir.getReturnValue());
    }
}
