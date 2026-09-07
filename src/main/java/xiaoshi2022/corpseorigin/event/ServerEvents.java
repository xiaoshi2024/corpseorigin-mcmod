package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;

/**
 * 服务端事件处理
 */
public final class ServerEvents {

    private ServerEvents() {
    }

    public static void register() {
        // 重生 → 同步角色
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            CharacterManager.getInstance().syncToClient(newPlayer);
        });

        // 登录 → 同步角色
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            CharacterManager.getInstance().syncToClient(player);
        });

        // 退出 → 清理
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // 空实现
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin server events registered");
    }
}