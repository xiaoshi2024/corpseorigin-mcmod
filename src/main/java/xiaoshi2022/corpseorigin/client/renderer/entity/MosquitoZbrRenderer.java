package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
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
 */
@Environment(EnvType.CLIENT)
public class MosquitoZbrRenderer extends GeoEntityRenderer<MosquitoZbrEntity, LivingEntityRenderState> {

    public MosquitoZbrRenderer(EntityRendererProvider.Context context) {
        super(context, new MosquitoZbrModel<>());
        this.shadowRadius = 0.25F;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, net.minecraft.resources.Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
