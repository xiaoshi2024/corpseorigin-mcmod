package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.YuDoctorModel;
import xiaoshi2022.corpseorigin.entity.YuDoctorEntity;
public class YuDoctorRenderer extends GeoEntityRenderer<YuDoctorEntity, LivingEntityRenderState> {
    public YuDoctorRenderer(EntityRendererProvider.Context context) {
        super(context, new YuDoctorModel());
        shadowRadius = .5f;
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }
}
