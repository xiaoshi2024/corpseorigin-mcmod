package xiaoshi2022.corpseorigin.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public final class CorpseNetwork {

    private CorpseNetwork() {
    }

    public static void register() {
        // ==================== 角色选择系统 ====================
        PayloadTypeRegistry.serverboundPlay().register(CorpsePayloads.SelectCharacterC2S.TYPE, CorpsePayloads.SelectCharacterC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.CharacterSyncS2C.TYPE, CorpsePayloads.CharacterSyncS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.PlayerCorpseSyncS2C.TYPE, CorpsePayloads.PlayerCorpseSyncS2C.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CorpsePayloads.SelectCharacterC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                xiaoshi2022.corpseorigin.character.CharacterManager.getInstance()
                        .setPlayerCharacter(player, payload.characterId());
            });
        });

        // ==================== 皮肤更新系统 ====================
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

    // ==================== ✅ 玩家尸兄数据同步 ====================

    /**
     * 发送玩家尸兄数据到客户端
     */
    public static void sendPlayerCorpseSync(ServerPlayer player) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);

        CompoundTag data = comp.getDataPublic();

        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                player.getId(),
                comp.isCorpse(),
                comp.getCorpseType(),
                data
        );

        ServerPlayNetworking.send(player, packet);
        CorpseOrigin.LOGGER.debug("同步玩家尸兄数据: {}", player.getName().getString());
    }

    /**
     * 广播玩家尸兄数据到所有玩家（用于转化时通知所有人）
     */
    public static void broadcastPlayerCorpseSync(ServerPlayer player) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);

        CompoundTag data = comp.getDataPublic();

        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                player.getId(),
                comp.isCorpse(),
                comp.getCorpseType(),
                data
        );

        // ✅ 通过 player.level().getServer() 获取服务器
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, packet);
            }
            CorpseOrigin.LOGGER.debug("广播玩家尸兄数据: {} 给 {} 个玩家",
                    player.getName().getString(),
                    server.getPlayerList().getPlayers().size());
        } else {
            // 如果获取服务器失败，至少发送给当前玩家
            ServerPlayNetworking.send(player, packet);
            CorpseOrigin.LOGGER.debug("广播失败，仅同步当前玩家: {}", player.getName().getString());
        }
    }

    public static void sendCharacterSync(ServerPlayer player, String characterId) {
        ServerPlayNetworking.send(player, new CorpsePayloads.CharacterSyncS2C(characterId));
    }
}