package xiaoshi2022.corpseorigin.client.renderer.item;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.item.weapon.GuigunClubItem;

/** 鬼棍·尸兄棍棒的三维物品渲染：模型与贴图取自 {@code geckolib/models/item/guigun_club.geo.json}。 */
public final class GuigunClubRenderer extends GeoItemRenderer<GuigunClubItem> {
    public GuigunClubRenderer() {
        super(new DefaultedItemGeoModel<>(CorpseOrigin.id("guigun_club")));
    }
}
