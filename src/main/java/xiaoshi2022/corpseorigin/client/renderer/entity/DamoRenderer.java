package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.DamoModel;
import xiaoshi2022.corpseorigin.entity.DamoEntity;
public class DamoRenderer extends GeoEntityRenderer<DamoEntity,LivingEntityRenderState>{public DamoRenderer(EntityRendererProvider.Context c){super(c,new DamoModel());shadowRadius=.4f;}}
