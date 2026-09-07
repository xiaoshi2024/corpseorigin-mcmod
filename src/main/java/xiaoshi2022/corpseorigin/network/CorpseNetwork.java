package xiaoshi2022.corpseorigin.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public final class CorpseNetwork {

    private CorpseNetwork() {
    }

    public static void register() {
        // ==================== 角色选择系统 ====================
        PayloadTypeRegistry.serverboundPlay().register(CorpsePayloads.SelectCharacterC2S.TYPE, CorpsePayloads.SelectCharacterC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.CharacterSyncS2C.TYPE, CorpsePayloads.CharacterSyncS2C.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CorpsePayloads.SelectCharacterC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                xiaoshi2022.corpseorigin.character.CharacterManager.getInstance()
                        .setPlayerCharacter(player, payload.characterId());
            });
        });

        // ==================== 皮肤更新系统 ====================
        // ✅ 使用独立的 ZbSkinUpdatePacket
        PayloadTypeRegistry.serverboundPlay().register(ZbSkinUpdatePacket.TYPE, ZbSkinUpdatePacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ZbSkinUpdatePacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (!(player.level() instanceof ServerLevel level)) return;

                Entity entity = level.getEntity(payload.entityId());
                if (entity instanceof LowerLevelZbEntity zbEntity) {
                    String textureStr = payload.skinTexture();
                    if (textureStr != null && !textureStr.isEmpty()) {
                        try {
                            Identifier texture = Identifier.fromNamespaceAndPath(
                                    textureStr.contains(":") ? textureStr.split(":")[0] : "corpseorigin",
                                    textureStr.contains(":") ? textureStr.split(":")[1] : textureStr
                            );
                            zbEntity.setSkinTextureFromServer(texture);
                        } catch (Exception e) {
                            CorpseOrigin.LOGGER.warn("无效的皮肤纹理路径: {}", textureStr);
                        }
                    }
                    zbEntity.setSkinStateFromServer(ZbSkinState.fromCode(payload.skinStateCode()));

                    CorpseOrigin.LOGGER.debug("服务端收到皮肤更新: 实体 {} 状态 {}",
                            payload.entityId(), payload.skinStateCode());
                }
            });
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin network registered (Fabric 26.2)");
    }

    public static void sendCharacterSync(ServerPlayer player, String characterId) {
        ServerPlayNetworking.send(player, new CorpsePayloads.CharacterSyncS2C(characterId));
    }
}