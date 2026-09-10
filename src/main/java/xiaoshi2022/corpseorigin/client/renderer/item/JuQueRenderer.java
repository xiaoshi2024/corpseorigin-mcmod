package xiaoshi2022.corpseorigin.client.renderer.item;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.item.sword.JuQue;

import static xiaoshi2022.corpseorigin.CorpseOrigin.MOD_ID;

public class JuQueRenderer extends GeoItemRenderer<JuQue> {
    public JuQueRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MOD_ID, "ming_juque_tw")));
    }
}
