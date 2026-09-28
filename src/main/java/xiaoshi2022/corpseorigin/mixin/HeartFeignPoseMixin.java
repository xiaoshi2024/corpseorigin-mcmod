package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant;

@Mixin(Player.class)
public class HeartFeignPoseMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$lieDown(CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (player.isAlive() && HeartImplant.active(player)) {
            player.setPose(Pose.SLEEPING);
            ci.cancel();
        }
    }
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private net.minecraft.world.phys.Vec3 corpseorigin$stayDown(net.minecraft.world.phys.Vec3 input) {
        Player player = (Player)(Object)this;
        // Keep vanilla gravity, water and knockback handling; suppress only movement input.
        return player.isAlive() && HeartImplant.active(player) ? net.minecraft.world.phys.Vec3.ZERO : input;
    }
}
