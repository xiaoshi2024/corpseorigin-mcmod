package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.RavenZbrModel;
import xiaoshi2022.corpseorigin.entity.RavenZbrEntity;

/**
 * 乌鸦尸兄绘制器 —— {@code raven_zbr} geo 资源。
 * 黑羽食腐鸦：地面踱步/打盹眨眼，饿了亮喙猎食，叫声只在飞行与闭眼窗口发出。
 */
@Environment(EnvType.CLIENT)
public class RavenZbrRenderer extends GeoEntityRenderer<RavenZbrEntity, LivingEntityRenderState> {

    public RavenZbrRenderer(EntityRendererProvider.Context context) {
        super(context, new RavenZbrModel<>());
        this.shadowRadius = 0.35F;
    }
}
