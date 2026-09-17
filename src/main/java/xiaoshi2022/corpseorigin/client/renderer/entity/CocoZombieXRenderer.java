package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.CocoZombieXModel;
import xiaoshi2022.corpseorigin.entity.CocoZombieXEntity;

public class CocoZombieXRenderer extends GeoEntityRenderer<CocoZombieXEntity, LivingEntityRenderState> {

    public CocoZombieXRenderer(EntityRendererProvider.Context context) {
        super(context, new CocoZombieXModel());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
