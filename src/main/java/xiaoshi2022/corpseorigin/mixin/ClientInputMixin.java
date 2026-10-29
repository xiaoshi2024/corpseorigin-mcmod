package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;

/**
 * 灵魂出窍期间本体的移动方向向量恒为零（配合 {@link KeyboardInputMixin} 清空按键状态）。
 */
@Mixin(ClientInput.class)
public abstract class ClientInputMixin {

    @Inject(method = "getMoveVector", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$noMoveWhileSoulProjecting(CallbackInfoReturnable<Vec2> cir) {
        if (PersistentCameraEntity.isSoulProjectionActive()) {
            cir.setReturnValue(Vec2.ZERO);
        }
    }
}
