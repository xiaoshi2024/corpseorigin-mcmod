package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.DoctorBeeRobotEntity;

public final class DoctorBeeRobotRenderer extends GeoEntityRenderer<DoctorBeeRobotEntity, LivingEntityRenderState> {
    public DoctorBeeRobotRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<DoctorBeeRobotEntity>(CorpseOrigin.id("doctor_bee_robot")) {
            @Override public Identifier getTextureResource(GeoRenderState state) {
                return Identifier.withDefaultNamespace("textures/entity/bee/bee.png");
            }
        });
        withScale(.55f);
        shadowRadius = .1f;
    }
}
