package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;
import xiaoshi2022.corpseorigin.client.model.block.CNChessZbrsModel;

/**
 * 象棋尸兄方块渲染器。
 * <p>
 * 方块实体用原版 {@link BlockEntityRenderState}，渲染类型在渲染器上指定。
 * <p>
 * 走子时通过渲染状态里的偏移票（{@link #MOVE_OFFSET}）把模型平移，
 * 配合 GeckoLib 数据票在"状态提取 → 渲染"之间传递数据，
 * 无需动画文件即可让棋子平滑滑动。
 */
public class CNChessZbrsRenderer extends GeoBlockRenderer<CNChessZbrsBlockEntity, BlockEntityRenderState> {

    /** 走子渲染偏移（格） */
    private static final DataTicket<Vec3> MOVE_OFFSET =
            DataTicket.create("corpseorigin.move_offset", Vec3.class);

    public CNChessZbrsRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new CNChessZbrsModel());
    }

    @Override
    public void extractRenderState(CNChessZbrsBlockEntity animatable, BlockEntityRenderState renderState,
                                    float partialTick, Vec3 cameraPos,
                                    CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(animatable, renderState, partialTick, cameraPos, crumblingOverlay);
        ClientLevel clientLevel = animatable.getLevel() instanceof ClientLevel cl ? cl : null;
        if (clientLevel != null) {
            Vec3 offset = animatable.getRenderOffset(clientLevel.getGameTime(), partialTick);
            renderState.getDataMap().put(MOVE_OFFSET, offset);
        }
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> passInfo) {
        Vec3 offset = passInfo.getOrDefaultGeckolibData(MOVE_OFFSET, Vec3.ZERO);
        if (offset.lengthSqr() > 1.0E-8) {
            passInfo.poseStack().translate(offset.x, offset.y, offset.z);
        }
        super.adjustRenderPose(passInfo);
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
