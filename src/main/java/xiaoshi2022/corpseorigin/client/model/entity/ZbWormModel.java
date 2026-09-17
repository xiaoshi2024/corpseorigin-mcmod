package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.ZbWormEntity;

public class ZbWormModel extends DefaultedEntityGeoModel<ZbWormEntity> {

    public ZbWormModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "zb_worm"));
    }


}
