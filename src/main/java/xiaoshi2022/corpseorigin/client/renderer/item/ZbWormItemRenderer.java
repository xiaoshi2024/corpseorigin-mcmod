package xiaoshi2022.corpseorigin.client.renderer.item;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.item.ZbWormItem;

import static xiaoshi2022.corpseorigin.CorpseOrigin.MOD_ID;

public class ZbWormItemRenderer extends GeoItemRenderer<ZbWormItem> {
    public ZbWormItemRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MOD_ID, "zb_worm_item")));
    }
}
