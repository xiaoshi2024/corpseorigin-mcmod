package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import xiaoshi2022.corpseorigin.client.model.entity.MosquitoZbrModel;
import xiaoshi2022.corpseorigin.entity.MosquitoZbrEntity;

/**
 * 蚊子尸兄（蚊群核心）绘制器 —— 复用 {@code mosquito_zbr} geo 资源，
 * Cutout 透明渲染（翅膀是半透面片）。
 * <p>
 * 缩放与 {@code MosquitoSwarmRenderer}（0.35 倍）完全一致：核心混在蚊群里看起来
 * 和普通蚊子一样小 —— "本体很小、移动极快、难以命中"，全靠血条暴露它。
 */
@Environment(EnvType.CLIENT)
public class MosquitoZbrRenderer extends GeoEntityRenderer<MosquitoZbrEntity, LivingEntityRenderState> {

    /** 与子蚊子一致的渲染缩放：整团蚊群里所有蚊子一样小 */
    private static final float CORE_SCALE = 0.35F;

    public MosquitoZbrRenderer(EntityRendererProvider.Context context) {
        super(context, new MosquitoZbrModel<>());
        this.shadowRadius = 0.1F;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, net.minecraft.resources.Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float width, float height) {
        info.poseStack().scale(CORE_SCALE, CORE_SCALE, CORE_SCALE);
    }
}
