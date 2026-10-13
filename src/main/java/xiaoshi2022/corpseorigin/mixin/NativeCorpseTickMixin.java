package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

@Mixin(LivingEntity.class)
public abstract class NativeCorpseTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void corpseorigin$tickIncubation(CallbackInfo ci) {
        if ((Object) this instanceof Mob mob) NativeCorpseState.tick(mob);
    }
}
