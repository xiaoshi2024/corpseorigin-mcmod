package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;
import xiaoshi2022.corpseorigin.client.model.block.ZBRFleshModel;

/**
 * 尸兄肉块的方块实体渲染器（GeckoLib 的 {@link GeoBlockRenderer}）。
 * <p>
 * 1.21.1 版是 {@code GeoBlockRenderer<ZBRFleshBlockEntity>}（只有一个泛型参数），
 * 5.5.5 起要显式带上渲染状态类型 —— 方块实体用原版的 {@link BlockEntityRenderState} 就够
 * （GeckoLib 已经把 GeoRenderState 混进了它）。
 * <p>
 * 渲染类型原来在模型里 {@code getRenderType(animatable, texture)}，现在统一挪到渲染器上、
 * 且签名换成了"渲染状态 + 贴图"（和 {@code LowerLevelZbRenderer} 一样）。
 */
public class ZBRFleshRenderer extends GeoBlockRenderer<ZBRFleshBlockEntity, BlockEntityRenderState> {

    public ZBRFleshRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new ZBRFleshModel());
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        // 26.2 的 RenderTypes 里没有 entityCutoutNoCull：不带 Cull 的 entityCutout 就是"不剔除面"那一档
        return RenderTypes.entityCutout(texture);
    }
}
