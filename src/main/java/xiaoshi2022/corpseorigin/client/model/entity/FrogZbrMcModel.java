package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 青蛙奇葩尸兄模型（{@code frog_zbr_mc} geo 资源，贴图 {@code textures/entity/frog_zbr_mc.png}）。
 * <p>
 * 动画键：idle / walk / attack / tongue_attack / cast / hurt / death /
 * was_shot_brains（骑手大脑被射出）/ not_brains（无脑状态）。
 *
 * @param <T> 青蛙奇葩尸兄
 */
@Environment(EnvType.CLIENT)
public class FrogZbrMcModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {

    public FrogZbrMcModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "frog_zbr_mc"));
    }
}
