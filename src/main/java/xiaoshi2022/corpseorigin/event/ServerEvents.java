package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

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
            // ★ 把在线其他玩家的尸兄状态补给刚进来的玩家。
            //   尸兄数据平时只在"发生变化"时广播，新玩家错过那些包的话，
            //   在他眼里别人就都是普通人（看不到多眼/外骨骼）。
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (!other.getUUID().equals(player.getUUID())) {
                    CorpseNetwork.sendPlayerCorpseSyncTo(other, player);
                }
            }
        });

        // 每 tick 末尾：把本 tick 改过尸兄数据的玩家统一广播一次
        // （所有 setter 都汇到 PlayerCorpseComponent.setData，那里只标脏不发包）
        ServerTickEvents.END_SERVER_TICK.register(server ->
                xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.flushPendingSync(server));

        // 退出 → 清理
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // 空实现
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin server events registered");
    }
}