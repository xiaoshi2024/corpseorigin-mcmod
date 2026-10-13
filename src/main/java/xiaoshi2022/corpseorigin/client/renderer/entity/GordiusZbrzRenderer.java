package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.GordiusZbrzModel;
import xiaoshi2022.corpseorigin.entity.GordiusZbrzEntity;

/** 寄生铁血虫尸兄渲染器。 */
public class GordiusZbrzRenderer extends GeoEntityRenderer<GordiusZbrzEntity, LivingEntityRenderState> {
    public GordiusZbrzRenderer(EntityRendererProvider.Context context) {
        super(context, new GordiusZbrzModel());
        shadowRadius = .4f;
    }
}
