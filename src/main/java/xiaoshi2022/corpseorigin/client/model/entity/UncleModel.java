package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.UncleEntity;

public class UncleModel extends DefaultedEntityGeoModel<UncleEntity> {

    public UncleModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "uncles"));
    }

}
