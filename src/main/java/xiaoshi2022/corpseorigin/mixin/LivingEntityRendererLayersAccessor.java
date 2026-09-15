package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * 读取实体渲染器已注册的渲染层列表。
 * <p>
 * 克隆分身的渲染器泛型与玩家渲染器一致（AvatarRenderState + PlayerModel），
 * 所以可以把玩家渲染器上的层（含其他模组挂上去的，如吸血鬼面部）复制一份过去。
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererLayersAccessor {

    @Accessor("layers")
    List<RenderLayer<?, ?>> getLayers();
}
