package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CocoPenguinEntity;

public class CocoPenguinModel extends DefaultedEntityGeoModel<CocoPenguinEntity> {

    public CocoPenguinModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "coco_penguin"));
    }

}
