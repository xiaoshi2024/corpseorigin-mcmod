package xiaoshi2022.corpseorigin.client.renderer.item;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.item.weapon.GuigunWeapItem;

/** 鬼棍·人类三节棍的三维物品渲染：模型与贴图取自 {@code geckolib/models/item/guigun_weap.geo.json}。 */
public final class GuigunWeapRenderer extends GeoItemRenderer<GuigunWeapItem> {
    public GuigunWeapRenderer() {
        super(new DefaultedItemGeoModel<>(CorpseOrigin.id("guigun_weap")));
    }
}
