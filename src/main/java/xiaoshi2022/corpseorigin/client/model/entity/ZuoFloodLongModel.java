package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.ZuoFloodLongEntity;

/**
 * 尸蛟龙（宠物 BOSS）的模型。
 * <p>
 * 用<b>它自己那套</b>资源（{@code zuo_flood_long}）：geo / 动画 / 贴图都和玩家蛟龙形态
 * （{@code zuo_guardian}）分开 —— 那套是"人骑在龙上"（带骑手骨、贴图里骑手叠玩家皮肤），
 * 这套是纯龙（{@code zuo_flood_long.geo.json} 里已经没有 riderx 那组骨），
 * 所以放出来当宠物时不会凭空多出一个人。
 * <p>
 * 动画名沿用同一套（{@code idle / reptile / riderx_attack / swim / raised ...}）。
 */
@Environment(EnvType.CLIENT)
public class ZuoFloodLongModel extends DefaultedEntityGeoModel<ZuoFloodLongEntity> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/zuo_flood_long.png");

    public ZuoFloodLongModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "zuo_flood_long"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }
}
