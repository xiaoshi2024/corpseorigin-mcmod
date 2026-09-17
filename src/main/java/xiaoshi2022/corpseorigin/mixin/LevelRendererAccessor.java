package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读 {@code LevelRenderer} 里那个私有的当帧渲染状态。
 * <p>
 * GeckoLib 渲染盔甲时要用 {@code levelRenderState.cameraRenderState}
 * （它内部也是这么取的），首人称手部渲染同理，而字段本身是私有的，只能开个访问器。
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("levelRenderState")
    LevelRenderState corpseorigin$levelRenderState();
}
