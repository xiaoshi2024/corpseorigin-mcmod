package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.minecraft.client.player.AbstractClientPlayer;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

public final class ShiChaoBodyRenderData {
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("shichao_body_active", Boolean.class);

    private ShiChaoBodyRenderData() {}

    public static boolean isActive(AbstractClientPlayer player) {
        if (player == null) return false;
        CorpseOriginClient.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(player.getUUID());
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_SHICHAOZHIZI;
    }
}
