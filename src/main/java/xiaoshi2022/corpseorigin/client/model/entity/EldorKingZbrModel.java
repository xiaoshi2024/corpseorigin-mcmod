package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.EldorKingZbrEntity;

/**
 * 尸兄·尔多兽王的模型 —— 二阶段（红毛）通过自定义 {@link DataTicket} 切换贴图。
 * <p>
 * 用法：{@link xiaoshi2022.corpseorigin.client.renderer.entity.EldorKingZbrRenderer#extractRenderState}
 * 在每帧把 {@code entity.isBerserk()} 写进 {@link #BERSERK_TICKET}，
 * 本类 {@link #getTextureResource(GeoRenderState)} 读 ticket，true 时切到红毛贴图。
 * <p>
 * 红毛 PNG 资源可暂缺：找不到时 GeckoLib 会回退到主贴图（仅 warn，不崩），
 * 二阶段至少还能靠 STR + 速度 ×1.4 + 攻速翻倍的视觉表现区分。
 */
@Environment(EnvType.CLIENT)
public class EldorKingZbrModel extends DefaultedEntityGeoModel<EldorKingZbrEntity> {

    /** 二阶段状态 ticket：renderer 写、model 读 */
    public static final DataTicket<Boolean> BERSERK_TICKET =
            DataTicket.create(CorpseOrigin.MOD_ID + ":eldor_berserk", Boolean.class);

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/eldor_king_zbr.png");
    private static final Identifier TEXTURE_BERSERK =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/eldor_king_zbr_berserk.png");

    public EldorKingZbrModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "eldor_king_zbr"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Boolean berserk = renderState.getGeckolibData(BERSERK_TICKET);
        return Boolean.TRUE.equals(berserk) ? TEXTURE_BERSERK : TEXTURE;
    }
}
