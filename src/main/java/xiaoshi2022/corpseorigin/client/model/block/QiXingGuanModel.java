package xiaoshi2022.corpseorigin.client.model.block;

import com.geckolib.model.DefaultedBlockGeoModel;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.QiXingGuanBlockEntity;

/**
 * 七星棺模型：geckolib/models/block/qi_xing_guan.geo.json
 * + textures/block/qi_xing_guan.png + geckolib/animations/block/qi_xing_guan.animation.json。
 */
public class QiXingGuanModel extends DefaultedBlockGeoModel<QiXingGuanBlockEntity> {

    public QiXingGuanModel() {
        super(CorpseOrigin.id("qi_xing_guan"));
    }
}
