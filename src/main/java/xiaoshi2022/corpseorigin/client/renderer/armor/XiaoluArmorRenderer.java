package xiaoshi2022.corpseorigin.client.renderer.armor;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.item.armor.XiaoluArmorItem;
public class XiaoluArmorRenderer<R extends HumanoidRenderState & com.geckolib.renderer.base.GeoRenderState> extends GeoArmorRenderer<XiaoluArmorItem,R> {
 public XiaoluArmorRenderer(){ super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID,"armor/xiaoluarmor"))); }
}
