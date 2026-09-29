package xiaoshi2022.corpseorigin.client.model.entity;
import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.MultiHeadCorpseWormEntity;
public class MultiHeadCorpseWormModel extends DefaultedEntityGeoModel<MultiHeadCorpseWormEntity> {
    public MultiHeadCorpseWormModel() { super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "multi_head_corpse_worm")); }
}
