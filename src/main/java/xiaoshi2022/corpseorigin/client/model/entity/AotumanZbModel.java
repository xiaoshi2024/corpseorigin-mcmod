package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.AotumanZbEntity;

/**
 * 凹凸曼尸兄的模型：<b>复用低阶尸兄整套几何与动画</b>，只把贴图换成固定的 {@code aotuman.png}。
 * <p>
 * 低阶尸兄平时走 {@link LowerLevelZbModel}，贴图是"玩家皮肤 + {@code lower_level_zb_render.png}
 * 尸化骨骼层"现合成的动态纹理；凹凸曼不参与那套合成，直接吃静态贴图 ——
 * 于是尸化叠加层带来的那只尸眼与脑后飘带都不会出现
 * （对应的骨头在 {@code AotumanZbRenderer} 里被跳过）。
 */
public class AotumanZbModel extends DefaultedEntityGeoModel<AotumanZbEntity> {

    /** 凹凸曼的固定贴图（64×64，玩家皮肤格式） */
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            CorpseOrigin.MOD_ID, "textures/entity/aotuman.png");

    public AotumanZbModel() {
        // 与低阶尸兄用同一个资源名 → 直接复用它的 geo.json / animation.json，不用另画一套
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "lower_level_zb"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }
}
