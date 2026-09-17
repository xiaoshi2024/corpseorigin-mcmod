package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.CocoZombieModel;
import xiaoshi2022.corpseorigin.entity.CocoZombieEntity;

public class CocoZombieRenderer extends GeoEntityRenderer<CocoZombieEntity, LivingEntityRenderState> {

    public CocoZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new CocoZombieModel());
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
