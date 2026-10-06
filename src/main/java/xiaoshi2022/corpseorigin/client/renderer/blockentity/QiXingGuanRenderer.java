package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.block.entity.QiXingGuanBlockEntity;
import xiaoshi2022.corpseorigin.client.model.block.QiXingGuanModel;

/**
 * 七星棺方块渲染器（GeckoLib）。开馆动画由方块实体控制器按 SUMMONED 状态驱动。
 */
public class QiXingGuanRenderer extends GeoBlockRenderer<QiXingGuanBlockEntity, BlockEntityRenderState> {

    public QiXingGuanRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new QiXingGuanModel());
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
