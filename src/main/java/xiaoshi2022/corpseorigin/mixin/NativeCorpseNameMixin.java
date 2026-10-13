package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.growth.NativeCorpseState;

@Mixin(Entity.class)
public abstract class NativeCorpseNameMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void corpseorigin$infectedSpeciesName(CallbackInfoReturnable<Component> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.getCustomName() == null && NativeCorpseState.infected(entity))
            cir.setReturnValue(Component.translatable("entity.corpseorigin.infected_native", cir.getReturnValue()));
    }
}
