package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
public class SkillConstructRenderer extends GeoEntityRenderer<SkillConstructEntity,LivingEntityRenderState> {
    public SkillConstructRenderer(EntityRendererProvider.Context context,String name){
        super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id(name)));shadowRadius=0;
    }
}
