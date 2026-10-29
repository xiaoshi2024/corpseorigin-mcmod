package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;

/**
 * 灵魂出窍期间，键盘读出来的按键状态全部清空。
 * <p>
 * 相机已经在灵魂上了，移动键全用来推灵魂；本体必须原地不动 —— 服务端虽然每 tick 把玩家
 * 拽回出窍点，但客户端自己走一步再被拽回来会来回抽搐。这里在键盘输入生成之后直接抹掉，
 * 本体就不会发移动包了。
 * <p>
 * 方向向量 {@code moveVector} 由 {@link ClientInputMixin} 负责清零。
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void corpseorigin$freezeWhileSoulProjecting(CallbackInfo ci) {
        if (!PersistentCameraEntity.isSoulProjectionActive()) {
            return;
        }
        ((ClientInput) (Object) this).keyPresses = Input.EMPTY;
    }
}
