package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.render.SwordImpactRenderer;

@Mixin(Camera.class)
public abstract class SwordCameraImpactMixin {
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();
    @Inject(method="alignWithEntity",at=@At("RETURN"))
    private void corpseorigin$swordShake(float partial,CallbackInfo ci){
        float yaw=SwordImpactRenderer.shake(0),pitch=SwordImpactRenderer.shake(1);
        if(yaw!=0 || pitch!=0)setRotation(yRot()+yaw,xRot()+pitch);
    }
}
