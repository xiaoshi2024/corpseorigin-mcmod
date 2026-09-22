package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CorpseAntEntity;

public class CorpseAntRenderer extends GeoEntityRenderer<CorpseAntEntity,LivingEntityRenderState> {
    public CorpseAntRenderer(EntityRendererProvider.Context context,String model){super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id(model)));shadowRadius=.4f;}
}
