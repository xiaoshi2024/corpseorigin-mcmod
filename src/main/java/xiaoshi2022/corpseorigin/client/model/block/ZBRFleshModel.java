package xiaoshi2022.corpseorigin.client.model.block;

import com.geckolib.model.DefaultedBlockGeoModel;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;

/**
 * 尸兄肉块的 Geo 模型。
 * <p>
 * 走 GeckoLib 的默认约定（{@link DefaultedBlockGeoModel}）：
 * <ul>
 *   <li>模型 {@code geckolib/models/block/zbr_flesh.geo.json}</li>
 *   <li>动画 {@code geckolib/animations/block/zbr_flesh.animation.json}</li>
 *   <li>贴图 {@code textures/block/zbr_flesh.png}</li>
 * </ul>
 */
public class ZBRFleshModel extends DefaultedBlockGeoModel<ZBRFleshBlockEntity> {

    public ZBRFleshModel() {
        super(CorpseOrigin.id("zbr_flesh"));
    }
}
