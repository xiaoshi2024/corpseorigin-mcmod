package xiaoshi2022.corpseorigin.event.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;

@Environment(EnvType.CLIENT)
public class AttackAnimationHandler {

    private static boolean wasSwinging = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!(client.player instanceof AbstractClientPlayer player)) return;

            if (player.swingTime > 0) {
                if (!wasSwinging) {
                    // ✅ 只触发本地玩家的渲染层
                    var renderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
                    if (renderer != null) {
                        ExoskeletonRenderLayer layer = CorpsePlayerRenderHandler.LAYER_MAP.get(renderer);
                        if (layer != null) {
                            layer.triggerSwing();
                        }
                    }
                    wasSwinging = true;
                }
            } else {
                wasSwinging = false;
            }
        });
    }
}