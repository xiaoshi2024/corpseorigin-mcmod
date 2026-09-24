package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suspend AI only while captured; never persist or overwrite the mob's NoAI flag. */
@Mixin(Mob.class)
public abstract class GourdCaptureMobMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void corpseorigin$holdCaptured(CallbackInfo ci){
        if(((Mob)(Object)this).getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.ANCHOR)>=0)ci.cancel();
    }
}
