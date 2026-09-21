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

    /** 这一帧的整身纹理（{@code shichaozhizi.png} 底图 + 顶上那具人形叠玩家皮肤）；缺省 = 用静态底图 */
    public static final DataTicket<Identifier> BODY_TEXTURE =
            DataTicket.create("shichao_body_texture", Identifier.class);

    /** 这一帧是不是正在放「千眼万目」—— 动画控制器据此整段改播 {@code special} */
    public static final DataTicket<Boolean> SPECIAL =
            DataTicket.create("shichao_body_special", Boolean.class);

    private ShiChaoBodyRenderData() {}

    public static boolean isActive(UUID uuid) {
        if (uuid == null) return false;
        CorpseOriginClient.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_SHICHAOZHIZI;
    }

    public static boolean isActive(AbstractClientPlayer player) {
        return player != null && isActive(player.getUUID());
    }
}
