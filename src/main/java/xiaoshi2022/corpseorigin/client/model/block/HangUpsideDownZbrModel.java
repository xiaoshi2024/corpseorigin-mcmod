package xiaoshi2022.corpseorigin.client.model.block;

import com.geckolib.model.DefaultedBlockGeoModel;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.HangUpsideDownZbrBlockEntity;

/**
 * 倒挂尸兄的 Geo 模型。
 * <p>
 * 走 GeckoLib 的默认约定（{@link DefaultedBlockGeoModel}）：
 * <ul>
 *   <li>模型 {@code geckolib/models/block/hang_upside_down_zbr.geo.json}</li>
 *   <li>动画 {@code geckolib/animations/block/hang_upside_down_zbr.animation.json}（idle / tongue）</li>
 *   <li>贴图 {@code textures/block/hang_upside_down_zbr.png}</li>
 * </ul>
 */
public class HangUpsideDownZbrModel extends DefaultedBlockGeoModel<HangUpsideDownZbrBlockEntity> {

    public HangUpsideDownZbrModel() {
        super(CorpseOrigin.id("hang_upside_down_zbr"));
    }
}
