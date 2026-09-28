package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.TianDoctorModel;
import xiaoshi2022.corpseorigin.entity.TianDoctorEntity;
public class TianDoctorRenderer extends GeoEntityRenderer<TianDoctorEntity,LivingEntityRenderState> {
    public TianDoctorRenderer(EntityRendererProvider.Context c){super(c,new TianDoctorModel());shadowRadius=.5f;
    }
}
