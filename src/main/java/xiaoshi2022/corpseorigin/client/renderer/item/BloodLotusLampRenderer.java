package xiaoshi2022.corpseorigin.client.renderer.item;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

import static xiaoshi2022.corpseorigin.CorpseOrigin.MOD_ID;

public class BloodLotusLampRenderer extends GeoItemRenderer<BloodLotusLamp> {
    public BloodLotusLampRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MOD_ID, "blood_lotus_lantern")));
    }
}
