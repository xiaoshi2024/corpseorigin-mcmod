package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.HamModel;
import xiaoshi2022.corpseorigin.entity.HamEntity;

public class HamRenderer extends GeoEntityRenderer<HamEntity, LivingEntityRenderState> {
    public HamRenderer(EntityRendererProvider.Context context) { super(context, new HamModel()); }
}
