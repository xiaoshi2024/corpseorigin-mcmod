package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 乌鸦尸兄模型（{@code raven_zbr} geo 资源，贴图 {@code textures/entity/raven_zbr.png}）。
 * <p>
 * 动画键：idle（地面清醒）/ eye_blink（打盹眨眼）/ fly（飞行）/ attack（啄击）/
 * feed（进食）。
 *
 * @param <T> 乌鸦尸兄
 */
@Environment(EnvType.CLIENT)
public class RavenZbrModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {

    public RavenZbrModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "raven_zbr"));
    }
}
