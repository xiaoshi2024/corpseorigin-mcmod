package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;

public class AttackAnimationHandler {

    private static boolean wasSwinging = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // ✅ 检测玩家是否在挥砍
            if (client.player.swingTime > 0) {
                if (!wasSwinging) {
                    // 刚触发攻击，播放外骨骼攻击动画
                    if (CorpsePlayerRenderHandler.renderLayerInstance != null) {
                        CorpsePlayerRenderHandler.renderLayerInstance.triggerSwing();
                    }
                    wasSwinging = true;
                }
            } else {
                wasSwinging = false;
            }
        });
    }
}