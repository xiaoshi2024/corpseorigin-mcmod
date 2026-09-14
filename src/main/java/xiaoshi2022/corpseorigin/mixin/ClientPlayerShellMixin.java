package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import xiaoshi2022.corpseorigin.client.camera.CameraLock;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;
import xiaoshi2022.corpseorigin.shell.ClientShell;
import xiaoshi2022.corpseorigin.shell.ShellState;


@Mixin(LocalPlayer.class)
public abstract class ClientPlayerShellMixin implements ClientShell, CameraLock {

    @Nullable
    @Override
    public String beginSync(ShellState state) {
        if (state == null || !state.isReady()) {
            return "目标身体尚未就绪";
        }
        return null;
    }

    @Override
    public void endSync(ShellState storedState) {
        // 由 CorpseOriginClient 的接收器处理
    }

    @Override
    public boolean isCameraLocked() {
        return Minecraft.getInstance().getCameraEntity() instanceof PersistentCameraEntity;
    }
}