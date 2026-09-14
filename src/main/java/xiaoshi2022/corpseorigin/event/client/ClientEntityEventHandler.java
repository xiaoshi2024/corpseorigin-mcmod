package xiaoshi2022.corpseorigin.event.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.client.renderer.entity.LowerLevelZbRenderer;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinCache;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;


@Environment(EnvType.CLIENT)
public class ClientEntityEventHandler {

    public static void register() {
        // ✅ 监听实体卸载
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof LowerLevelZbEntity) {
                LowerLevelZbRenderer.onEntityRemoved(entity.getId());
            }
        });

        // ✅ 玩家退出世界时清空所有缓存
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(() -> {   // ★ 切到客户端主线程
                // ★ 兜底：确保 HUD 不会被卡在隐藏状态
                if (client.gui.hud.isHidden()) {
                    client.gui.hud.toggle();
                }
                LowerLevelZbRenderer.clearCache();
                ZbSkinCache.clearAll();
                CorpseOriginClient.tempRedEyeTicks.clear();
                ExoskeletonRenderLayer.clearCache();
                CorpseOriginClient.corpseDataCache.clear();
                CorpseOrigin.LOGGER.info("🧹 玩家退出世界，清空所有缓存");
            });
        });
    }
}