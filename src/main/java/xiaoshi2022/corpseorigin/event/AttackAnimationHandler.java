package xiaoshi2022.corpseorigin.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;

@Environment(EnvType.CLIENT)
public class AttackAnimationHandler {

    private static boolean wasSwinging = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (client.player.swingTime > 0) {
                if (!wasSwinging) {
                    // ✅ 触发所有渲染层的挥砍动画
                    CorpsePlayerRenderHandler.triggerSwingAll();
                    wasSwinging = true;
                }
            } else {
                wasSwinging = false;
            }
        });
    }
}