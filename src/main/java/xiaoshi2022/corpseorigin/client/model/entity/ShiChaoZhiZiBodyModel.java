package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;

public final class ShiChaoZhiZiBodyModel extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {
    public ShiChaoZhiZiBodyModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "shichaozhizi"));
    }
}
