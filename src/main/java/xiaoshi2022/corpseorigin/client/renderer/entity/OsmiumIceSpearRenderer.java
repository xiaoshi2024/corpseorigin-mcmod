package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.entity.OsmiumIceSpearEntity;
import xiaoshi2022.corpseorigin.CorpseOrigin;
public class OsmiumIceSpearRenderer extends GeoEntityRenderer<OsmiumIceSpearEntity, LivingEntityRenderState> {
    public OsmiumIceSpearRenderer(EntityRendererProvider.Context context) { super(context, new DefaultedEntityGeoModel<>(CorpseOrigin.id("osmium_ice_spear"))); shadowRadius = 0; }
}
