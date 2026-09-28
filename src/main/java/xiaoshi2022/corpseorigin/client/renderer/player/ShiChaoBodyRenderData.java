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

    /** 本帧尸巢之子的整身纹理；缺少动态纹理时使用静态底图。 */
    public static final DataTicket<Identifier> BODY_TEXTURE =
            DataTicket.create("shichao_body_texture", Identifier.class);

    /** 本帧是否正在播放「千眼万目」，供动画控制器选择 {@code special} 动画。 */
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
