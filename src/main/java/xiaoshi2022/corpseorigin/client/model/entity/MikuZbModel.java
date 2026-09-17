package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.MikuZbEntity;

/**
 * 初音尸兄模型 —— 复用低级尸兄的人形骨架（idle / walk / attack），
 * 贴图为青绿色双马尾配色的 miku_zb.png，后续可替换正式美术。
 */
public class MikuZbModel extends DefaultedEntityGeoModel<MikuZbEntity> {

    public MikuZbModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "miku_zb"));
    }
}
