package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant;

@Mixin(LivingEntity.class)
public class HeartFeignJumpMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$noJump(CallbackInfo ci) {
        if ((Object)this instanceof Player p && HeartImplant.active(p)) ci.cancel();
    }
}
