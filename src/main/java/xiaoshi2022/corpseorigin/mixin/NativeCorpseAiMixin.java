package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.entity.ai.NativeCorpseAi;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

@Mixin(Mob.class)
public abstract class NativeCorpseAiMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$huntAsCorpse(CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        if (NativeCorpseState.infected(mob) && mob.level() instanceof ServerLevel level) {
            NativeCorpseAi.tick(mob, level);
            ci.cancel();
        }
    }
}
