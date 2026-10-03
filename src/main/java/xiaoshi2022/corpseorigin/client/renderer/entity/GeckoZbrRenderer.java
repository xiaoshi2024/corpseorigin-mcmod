package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.GeckoZbrModel;
import xiaoshi2022.corpseorigin.entity.GeckoZbrEntity;

/**
 * 壁虎奇葩尸兄绘制器 —— {@code gecko_zbr} geo 资源。
 * 金色长尾壁虎 + 头顶两颗人脸小脑袋（zbr_head → L/R），断尾后由 grow 动画再生。
 */
@Environment(EnvType.CLIENT)
public class GeckoZbrRenderer extends GeoEntityRenderer<GeckoZbrEntity, LivingEntityRenderState> {

    public GeckoZbrRenderer(EntityRendererProvider.Context context) {
        super(context, new GeckoZbrModel<>());
        this.shadowRadius = 0.7F;
    }
}
