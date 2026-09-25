package xiaoshi2022.corpseorigin.compat.flashback.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import xiaoshi2022.corpseorigin.compat.flashback.ReplaySnapshotInjector;

/**
 * 挂钩 Flashback 录制端的快照扩展点 Recorder#writeCustomSnapshot(Consumer)。
 * <p>
 * 该方法在 0.43.4 中是空实现，但每次写快照（录制开始 + 回放块边界）都会被调用，
 * 传入的 consumer 会把接收的 S2C 包追加进快照包列表并写入录像。
 * 用 targets 字符串而非 value = Recorder.class，使本模组在编译期无需 Flashback 依赖；
 * 是否应用由 {@link xiaoshi2022.corpseorigin.compat.flashback.FlashbackMixinPlugin} 门控。
 */
@Mixin(targets = "com.moulberry.flashback.record.Recorder")
public class RecorderMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "writeCustomSnapshot", at = @At("HEAD"))
    private void corpseorigin$injectReplayState(Consumer consumer, CallbackInfo ci) {
        ReplaySnapshotInjector.inject(consumer);
    }
}
