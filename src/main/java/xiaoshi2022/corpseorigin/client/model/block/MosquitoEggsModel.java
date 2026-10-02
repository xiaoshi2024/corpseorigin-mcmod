package xiaoshi2022.corpseorigin.client.model.block;

import com.geckolib.model.DefaultedBlockGeoModel;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.MosquitoEggsBlockEntity;

/**
 * 蚊子尸兄卵的 Geo 模型（套路同 {@code ZBRFleshModel}）。
 * <p>
 * 走 GeckoLib 的默认约定（{@link DefaultedBlockGeoModel}）：
 * <ul>
 *   <li>模型 {@code geckolib/models/block/mosquito_zbr_eggs.geo.json}</li>
 *   <li>动画 {@code geckolib/animations/block/mosquito_zbr_eggs.animation.json}
 *       （idle / wriggle / incubate，按方块 age 在 BlockEntity 里分段）</li>
 *   <li>贴图 {@code textures/block/mosquito_zbr_eggs.png}</li>
 * </ul>
 */
public class MosquitoEggsModel extends DefaultedBlockGeoModel<MosquitoEggsBlockEntity> {

    public MosquitoEggsModel() {
        super(CorpseOrigin.id("mosquito_zbr_eggs"));
    }
}
