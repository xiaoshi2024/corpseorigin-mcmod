package xiaoshi2022.corpseorigin.client.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 标记：相机接管时，玩家的视角输入被拦截。
 * 由 ClientPlayerShellMixin 实现。
 */
@Environment(EnvType.CLIENT)
public interface CameraLock {

    /** true 表示当前相机被 PersistentCameraEntity 接管 */
    boolean isCameraLocked();
}