package xiaoshi2022.corpseorigin.mixin;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Projectile.class)
public abstract class NestProjectileMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$intercept(CallbackInfo ci) {
        if (xiaoshi2022.corpseorigin.skill.longyou.NestDefense.intercept((Projectile)(Object)this)) ci.cancel();
    }
}
