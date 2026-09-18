package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.item.CharacterBookItem;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.UUID;

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
            // 重生是换了一个新的玩家实体，尸王的基础数值要重新套上（不是龙右就是摘掉）
            xiaoshi2022.corpseorigin.character.LongYou.applyIfLongYou(newPlayer);
            // ★ 体型也要复位：死在"拇指原体"里重生，SCALE 属性会被一起带过来，
            //   不复位的话人会一直是个小人儿
            xiaoshi2022.corpseorigin.character.LongYou.resetBodySize(newPlayer);
        });

        // 登录 → 同步角色
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            CharacterManager.getInstance().syncToClient(player);
            // 同理：补一次尸王基础数值，避免老存档里已经切到龙右的玩家没数值
            xiaoshi2022.corpseorigin.character.LongYou.applyIfLongYou(player);
            // ★ 把在线其他玩家的尸兄状态补给刚进来的玩家。
            //   尸兄数据平时只在"发生变化"时广播，新玩家错过那些包的话，
            //   在他眼里别人就都是普通人（看不到多眼/外骨骼）。
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (!other.getUUID().equals(player.getUUID())) {
                    CorpseNetwork.sendPlayerCorpseSyncTo(other, player);
                }
            }
            // 生存开局：第一次进服的玩家发一本统一角色书
            giveStarterBookOnce(player);
        });

        // 每 tick 末尾：把本 tick 改过尸兄数据的玩家统一广播一次
        // （所有 setter 都汇到 PlayerCorpseComponent.setData，那里只标脏不发包）
        ServerTickEvents.END_SERVER_TICK.register(server ->
                xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.flushPendingSync(server));

        // 每 tick 推进天线宝宝盔甲的吸食（抓取 → 持续吸血 → 松手/被打断）
        // 玩家和穿戴该套装的生物共用同一套逻辑
        ServerTickEvents.END_SERVER_TICK.register(
                xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.EntityAntennaSuckHandler::tick);

        // 每 tick 推进尸王次声波声场（频率扫描震散投掷物 + 持续指派被操控的尸兄）
        ServerTickEvents.END_SERVER_TICK.register(
                xiaoshi2022.corpseorigin.skill.longyou.InfrasoundFieldHandler::tick);

        // 每 tick 推进尸王的球状闪电（飞行 → 放电 → 炸开）
        ServerTickEvents.END_SERVER_TICK.register(
                xiaoshi2022.corpseorigin.skill.longyou.ThunderStrikeHandler::tick);

        // 金蝉脱壳后：右键旧身体穿回去（只在缩在原体里的时候接管）
        xiaoshi2022.corpseorigin.skill.longyou.BodyTransplantHandler.register();

        // 退出 → 清理
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // 空实现
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin server events registered");
    }

    /**
     * 生存开局送一本「统一角色书」—— 右键打开选人界面，从全部已注册角色里挑一位，选完书消失。
     * <p>
     * <b>创造模式不发</b>（他们能直接从创造页签里拿），而且这一条写在"打标记"之前，
     * 所以创造模式的玩家只是<b>这次不发</b>，并没有被记成"已领过"——
     * 等他哪天转生存了，下次登录照样能领到。
     * <p>
     * 每个玩家只发一次：<b>发的那一刻</b>就在 {@link PlayerCharacterData} 里打上标记（跟随世界存档，
     * 并且跟着换身搬家的那条 NBT 通路一起走），所以重复登录、死亡重生、换身都不会再收到第二本。
     * <p>
     * 老存档里"从没领过"的玩家下次登录会补一本 —— 不然他们没有任何选角色的入口。
     * 背包满时 {@code placeItemBackInInventory} 会把它丢在脚下，不会凭空消失。
     */
    private static void giveStarterBookOnce(ServerPlayer player) {
        if (player.isCreative()) {
            return;
        }

        PlayerCharacterData data = PlayerCharacterData.get(player);
        UUID uuid = player.getUUID();
        if (data.hasReceivedStarterBook(uuid)) {
            return;
        }

        data.markStarterBookReceived(uuid);
        player.getInventory().placeItemBackInInventory(CharacterBookItem.createUnboundStack());
        player.sendOverlayMessage(Component.translatable("message.corpseorigin.character_book.starter")
                .withStyle(ChatFormatting.GOLD));
    }
}