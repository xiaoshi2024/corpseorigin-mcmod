package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.EldorKingZbrModel;
import xiaoshi2022.corpseorigin.entity.EldorKingZbrEntity;

/**
 * 尸兄·尔多兽王绘制器。
 * <p>
 * 二阶段（红毛）切换：每帧 {@link #extractRenderState} 把 {@code entity.isBerserk()}
 * 写进 {@link EldorKingZbrModel#BERSERK_TICKET}，模型按它切贴图。
 * <p>
 * 缩放、阴影、cutout 渲染走默认实现（{@link GeoEntityRenderer} 自动按 SCALE 属性缩放）。
 */
@Environment(EnvType.CLIENT)
public class EldorKingZbrRenderer extends GeoEntityRenderer<EldorKingZbrEntity, LivingEntityRenderState> {

    public EldorKingZbrRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        super(context, new EldorKingZbrModel());
        this.shadowRadius = 0.9F;
    }

    @Override
    public void extractRenderState(EldorKingZbrEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // 把二阶段标志写进 GeckoLib 状态 ticket，模型 getTextureResource 读它切贴图
        state.addGeckolibData(EldorKingZbrModel.BERSERK_TICKET, entity.isBerserk());
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
