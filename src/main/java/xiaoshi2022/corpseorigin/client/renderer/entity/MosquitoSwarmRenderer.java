package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.MosquitoZbrModel;
import xiaoshi2022.corpseorigin.entity.MosquitoSwarmEntity;

/**
 * 环绕蚊子绘制器 —— 与蚊群核心共用 {@code mosquito_zbr} geo / 贴图，
 * 只是整体缩到 0.35 倍（缩放套路同 {@code MultiHeadCorpseWormRenderer}）。
 */
@Environment(EnvType.CLIENT)
public class MosquitoSwarmRenderer extends GeoEntityRenderer<MosquitoSwarmEntity, LivingEntityRenderState> {

    /** 子蚊子相对核心的渲染缩放 */
    private static final float SWARM_SCALE = 0.35F;

    public MosquitoSwarmRenderer(EntityRendererProvider.Context context) {
        super(context, new MosquitoZbrModel<>());
        this.shadowRadius = 0.05F;
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float width, float height) {
        info.poseStack().scale(SWARM_SCALE, SWARM_SCALE, SWARM_SCALE);
    }

    @Override
    public net.minecraft.client.renderer.rendertype.RenderType getRenderType(
            LivingEntityRenderState renderState, net.minecraft.resources.Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture);
    }
}
