package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;

/**
 * 客户端初始化
 */
public class CorpseOriginClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. 按键
        CorpseKeyBindings.register();

        // 2. HUD
        // TODO: 后续恢复 HUD

        // 3. 网络接收
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CharacterSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    CharacterManagerBridge.setCharacter(payload.characterId()));
        });

        // TODO: 后续恢复其他同步

        CorpseOrigin.LOGGER.info("CorpseOrigin client initialized");
    }
}