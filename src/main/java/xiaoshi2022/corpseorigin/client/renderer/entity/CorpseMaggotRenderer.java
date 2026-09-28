package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CorpseMaggotEntity;
public final class CorpseMaggotRenderer extends GeoEntityRenderer<CorpseMaggotEntity,LivingEntityRenderState> {
    public CorpseMaggotRenderer(EntityRendererProvider.Context c){super(c,new DefaultedEntityGeoModel<>(CorpseOrigin.id("corpse_maggot")));shadowRadius=.2f;}
}
