package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.entity.HamEntity;

public class HamModel extends DefaultedEntityGeoModel<HamEntity> {
    public HamModel() { super(Identifier.fromNamespaceAndPath("corpseorigin", "ham")); }
}
