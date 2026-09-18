package xiaoshi2022.corpseorigin.client.renderer.armor;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.item.armor.LongYouClothItem;

/**
 * 尸王专属服装（龙右）渲染器。
 * <p>
 * 模型 / 动画 / 贴图全走 GeckoLib 默认约定（{@code armor/longyoucloth}）：
 * <ul>
 *   <li>{@code geckolib/models/item/armor/longyoucloth.geo.json}</li>
 *   <li>{@code geckolib/animations/item/armor/longyoucloth.animation.json}</li>
 *   <li>{@code textures/item/armor/longyoucloth.png}</li>
 * </ul>
 * 这套没有发光贴图，所以不加 {@code AutoGlowingGeoLayer}（天线宝宝那套有 glowmask 才加）。
 */
public class LongYouClothArmorRenderer<R extends HumanoidRenderState & GeoRenderState>
        extends GeoArmorRenderer<LongYouClothItem, R> {

    public LongYouClothArmorRenderer() {
        super(new DefaultedItemGeoModel<>(
                Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "armor/longyoucloth")));
    }
}
