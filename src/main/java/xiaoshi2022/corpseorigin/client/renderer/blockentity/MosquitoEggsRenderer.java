package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.block.entity.MosquitoEggsBlockEntity;
import xiaoshi2022.corpseorigin.client.model.block.MosquitoEggsModel;

/**
 * 蚊子尸兄卵的方块实体渲染器（GeckoLib 的 {@link GeoBlockRenderer}，套路同 {@code ZBRFleshRenderer}）。
 * <p>
 * 卵块带半透明卵壳面，用不带剔除的 entityCutout。
 */
public class MosquitoEggsRenderer extends GeoBlockRenderer<MosquitoEggsBlockEntity, BlockEntityRenderState> {

    public MosquitoEggsRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new MosquitoEggsModel());
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
