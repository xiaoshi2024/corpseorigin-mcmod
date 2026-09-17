package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CocoZombieEntity;

public class CocoZombieModel extends DefaultedEntityGeoModel<CocoZombieEntity> {

    public CocoZombieModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "coco_penguin_zbr"));
    }


}
