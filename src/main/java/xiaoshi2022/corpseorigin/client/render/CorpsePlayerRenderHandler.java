package xiaoshi2022.corpseorigin.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.mixin.LivingEntityRendererMixin;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

@Environment(EnvType.CLIENT)
public final class CorpsePlayerRenderHandler {

    private static boolean registered = false;

    private CorpsePlayerRenderHandler() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!registered && client.player != null) {
                registered = true;
                registerLayers(client);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerLayers(Minecraft client) {
        var entityRenderDispatcher = client.getEntityRenderDispatcher();
        if (entityRenderDispatcher == null) {
            registered = false;
            return;
        }

        if (client.player instanceof AbstractClientPlayer abstractPlayer) {
            var playerRenderer = entityRenderDispatcher.getPlayerRenderer(abstractPlayer);
            if (playerRenderer != null) {
                var modelSet = Minecraft.getInstance().getEntityModels();
                var exoskeletonModel = new ExoskeletonModel(modelSet.bakeLayer(ModModelLayers.EXOSKELETON));

                // ✅ 直接使用 Mixin 调用，忽略泛型警告
                var mixin = (LivingEntityRendererMixin) playerRenderer;
                mixin.callAddLayer(new ExoskeletonRenderLayer(playerRenderer, exoskeletonModel));

                CorpseOrigin.LOGGER.info("✅ 尸兄玩家外骨骼渲染层已注册");
            } else {
                registered = false;
            }
        } else {
            registered = false;
        }
    }
}