package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.block.entity.HangUpsideDownZbrBlockEntity;
import xiaoshi2022.corpseorigin.client.model.block.HangUpsideDownZbrModel;

/**
 * 倒挂尸兄方块渲染器（GeckoLib）。吐舌动画由方块实体控制器按 TONGUE 方块状态驱动。
 */
public class HangUpsideDownZbrRenderer extends GeoBlockRenderer<HangUpsideDownZbrBlockEntity, BlockEntityRenderState> {

    public HangUpsideDownZbrRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new HangUpsideDownZbrModel());
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
