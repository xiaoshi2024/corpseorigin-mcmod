package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CocoZombieXEntity;


public class CocoZombieXModel extends DefaultedEntityGeoModel<CocoZombieXEntity> {

    public CocoZombieXModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "coco_penguin_zbrx"));
    }

    // 如果贴图路径和默认推导不一致，才需要重写 getTextureResource
    // 默认推导：assets/corpseorigin/textures/entity/coco_penguin_zbrx.png
    // 如果你的贴图确实在这个位置，也可以删掉下面的重写
}