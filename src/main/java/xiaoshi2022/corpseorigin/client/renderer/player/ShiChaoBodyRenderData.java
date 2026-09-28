package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.UUID;

public final class ShiChaoBodyRenderData {
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("shichao_body_active", Boolean.class);

    /** 杩欎竴甯х殑鏁磋韩绾圭悊锛坽@code shichaozhizi.png} 搴曞浘 + 椤朵笂閭ｅ叿浜哄舰鍙犵帺瀹剁毊鑲わ級锛涚己鐪?= 鐢ㄩ潤鎬佸簳鍥?*/
    public static final DataTicket<Identifier> BODY_TEXTURE =
            DataTicket.create("shichao_body_texture", Identifier.class);

    /** 杩欎竴甯ф槸涓嶆槸姝ｅ湪鏀俱€屽崈鐪间竾鐩€嶁€斺€?鍔ㄧ敾鎺у埗鍣ㄦ嵁姝ゆ暣娈垫敼鎾?{@code special} */
    public static final DataTicket<Boolean> SPECIAL =
            DataTicket.create("shichao_body_special", Boolean.class);

    private ShiChaoBodyRenderData() {}

    public static boolean isActive(UUID uuid) {
        if (uuid == null) return false;
        xiaoshi2022.corpseorigin.client.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_SHICHAOZHIZI;
    }

    public static boolean isActive(AbstractClientPlayer player) {
        return player != null && isActive(player.getUUID());
    }
}
