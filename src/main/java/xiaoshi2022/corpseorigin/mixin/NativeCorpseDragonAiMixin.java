package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.entity.ai.NativeCorpseAi;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

@Mixin(EnderDragon.class)
public abstract class NativeCorpseDragonAiMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void corpseorigin$corpsePrey(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon)(Object)this;
        if (NativeCorpseState.infected(dragon) && dragon.level() instanceof ServerLevel level)
            NativeCorpseAi.dragon(dragon, level);
    }
}
