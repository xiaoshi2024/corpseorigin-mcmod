package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.GordiusZbrzEntity;

/** 寄生铁血虫尸兄：鱼体 + 藏在鱼腹里的铁线虫（idle/show/wizened/swing 四动画）。 */
public class GordiusZbrzModel extends DefaultedEntityGeoModel<GordiusZbrzEntity> {
    public GordiusZbrzModel() { super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "gordius_zbrz")); }
}
