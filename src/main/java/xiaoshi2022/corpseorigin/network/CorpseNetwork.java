package xiaoshi2022.corpseorigin.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class CorpseNetwork {

    private CorpseNetwork() {
    }

    public static void register() {
        // 注册编解码
        PayloadTypeRegistry.serverboundPlay().register(CorpsePayloads.SelectCharacterC2S.TYPE, CorpsePayloads.SelectCharacterC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.CharacterSyncS2C.TYPE, CorpsePayloads.CharacterSyncS2C.CODEC);

        // 服务端接收 - 选择角色
        ServerPlayNetworking.registerGlobalReceiver(CorpsePayloads.SelectCharacterC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                // 通过 CharacterManager 处理
                xiaoshi2022.corpseorigin.character.CharacterManager.getInstance()
                        .setPlayerCharacter(player, payload.characterId());
            });
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin network registered");
    }

    public static void sendCharacterSync(ServerPlayer player, String characterId) {
        ServerPlayNetworking.send(player, new CorpsePayloads.CharacterSyncS2C(characterId));
    }
}