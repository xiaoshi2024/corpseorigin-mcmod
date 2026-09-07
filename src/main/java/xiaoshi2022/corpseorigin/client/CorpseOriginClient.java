package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.LowerLevelZbRenderer;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/**
 * 客户端初始化 - Fabric 26.2
 */
public class CorpseOriginClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. 按键绑定
        CorpseKeyBindings.register();

        // 2. 实体渲染器注册
        EntityRendererRegistry.register(ModEntities.LOWER_LEVEL_ZB, LowerLevelZbRenderer::new);

        // 3. HUD
        // TODO: 后续恢复 HUD

        // 4. 网络接收 - 角色同步
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CharacterSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    CharacterManagerBridge.setCharacter(payload.characterId()));
        });

        // TODO: 后续恢复其他同步（感染、技能等）

        CorpseOrigin.LOGGER.info("CorpseOrigin client initialized");
    }
}