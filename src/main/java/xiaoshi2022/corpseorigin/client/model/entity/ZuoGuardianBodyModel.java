package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.renderer.player.MutantBodyRenderData;

/**
 * 左护法变异体（巨蛇 + 骑手）打在玩家身上用的模型。
 * <p>
 * geo / 动画都直接取
 * {@code geckolib/models/entity/zuo_guardian.geo.json} 与
 * {@code geckolib/animations/entity/zuo_guardian.animation.json}（同一个 id 派生，不用另外配置）。
 * <p>
 * 纹理不是固定的 zuo_guardian.png，而是每帧由 {@code ZuoGuardianSkinBuilder} 合成的
 * 「底图 + 骑手处玩家皮肤」，通过 render state 带进来（见 {@link MutantBodyRenderData#BODY_TEXTURE}）。
 */
@Environment(EnvType.CLIENT)
public class ZuoGuardianBodyModel extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {

    /** 合成纹理还没就绪时的兜底：至少画出底图，别变成紫黑格 */
    private static final Identifier BASE_TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/zuo_guardian.png");

    public ZuoGuardianBodyModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "zuo_guardian"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Identifier texture = renderState.getGeckolibData(MutantBodyRenderData.BODY_TEXTURE);
        return texture != null ? texture : BASE_TEXTURE;
    }
}
