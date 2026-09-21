package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CorpseFishEggEntity;
public class CorpseFishEggRenderer extends GeoEntityRenderer<CorpseFishEggEntity,LivingEntityRenderState> {
    public CorpseFishEggRenderer(EntityRendererProvider.Context context) {
        super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id("corpse_fish_egg"))); shadowRadius=0;
    }
}
