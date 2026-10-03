package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 壁虎奇葩尸兄模型（{@code gecko_zbr} geo 资源，贴图 {@code textures/entity/gecko_zbr.png}）。
 * <p>
 * 动画键：idle / crawl / attack / tongue_attack / no_tail（断尾）/ not_tail（无尾保持）/
 * grow（尾巴再生）。
 *
 * @param <T> 壁虎奇葩尸兄
 */
@Environment(EnvType.CLIENT)
public class GeckoZbrModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {

    public GeckoZbrModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "gecko_zbr"));
    }
}
