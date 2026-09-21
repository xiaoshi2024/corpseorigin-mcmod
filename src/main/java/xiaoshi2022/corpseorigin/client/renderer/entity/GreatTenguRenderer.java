package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.GreatTenguEntity;
public class GreatTenguRenderer extends GeoEntityRenderer<GreatTenguEntity,LivingEntityRenderState>{
    public GreatTenguRenderer(EntityRendererProvider.Context context){super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id("great_tengu")));shadowRadius=0;}
}
