package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.entity.ZishuRobotEntity;
import xiaoshi2022.corpseorigin.CorpseOrigin;
public final class ZishuRobotRenderer extends GeoEntityRenderer<ZishuRobotEntity,LivingEntityRenderState>{
    public ZishuRobotRenderer(EntityRendererProvider.Context c){super(c,new DefaultedEntityGeoModel<>(CorpseOrigin.id("zishu_robot")));shadowRadius=.45f;}
}
