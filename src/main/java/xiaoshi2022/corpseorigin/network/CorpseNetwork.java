package xiaoshi2022.corpseorigin.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CorpseNetwork {

    // ✅ 待同步队列
    private static final Set<UUID> PENDING_SYNC = new HashSet<>();

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

        // ==================== ✅ 玩家加入时加入待同步队列 ====================
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joiningPlayer = handler.getPlayer();
            PENDING_SYNC.add(joiningPlayer.getUUID());
            CorpseOrigin.LOGGER.info("玩家 {} 加入，加入待同步队列",
                    joiningPlayer.getName().getString());
        });

        // ✅ 玩家退出时移除
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PENDING_SYNC.remove(handler.getPlayer().getUUID());
        });

        // ✅ 每 tick 检查待同步队列（延迟 20 tick 后同步）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING_SYNC.isEmpty()) return;

            var iterator = PENDING_SYNC.iterator();
            while (iterator.hasNext()) {
                UUID uuid = iterator.next();
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);

                if (player == null) {
                    iterator.remove();
                    continue;
                }

                // ✅ 玩家 tickCount >= 20 后同步
                if (player.tickCount >= 20) {
                    iterator.remove();
                    syncAllCorpseDataToPlayer(player, server);
                    CorpseOrigin.LOGGER.info("✅ 延迟同步完成: {}",
                            player.getName().getString());
                }
            }
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin network registered (Fabric 26.2)");
    }

    /**
     * ✅ 同步所有尸兄玩家数据给新加入的玩家（包括自己）
     */
    private static void syncAllCorpseDataToPlayer(ServerPlayer joiningPlayer, MinecraftServer server) {
        // ✅ 1. 先给自己发自己的数据
        PlayerCorpseComponent selfComp = PlayerCorpseComponent.get(joiningPlayer);
        if (selfComp.isCorpse()) {
            CompoundTag selfData = selfComp.getDataPublic();
            CorpsePayloads.PlayerCorpseSyncS2C selfPacket = new CorpsePayloads.PlayerCorpseSyncS2C(
                    joiningPlayer.getUUID(),
                    selfComp.isCorpse(),
                    selfComp.getCorpseType(),
                    selfData
            );
            ServerPlayNetworking.send(joiningPlayer, selfPacket);
            CorpseOrigin.LOGGER.info("✅ 已发送自己的尸兄数据给 {}",
                    joiningPlayer.getName().getString());
        }

        // ✅ 2. 发其他尸兄玩家的数据
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == joiningPlayer) continue;

            PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
            if (comp.isCorpse()) {
                CompoundTag data = comp.getDataPublic();
                CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                        player.getUUID(),
                        comp.isCorpse(),
                        comp.getCorpseType(),
                        data
                );
                ServerPlayNetworking.send(joiningPlayer, packet);
                count++;
            }
        }

        // ✅ 3. 如果自己是尸兄，广播给所有人
        if (selfComp.isCorpse()) {
            broadcastPlayerCorpseSync(joiningPlayer);
        }

        CorpseOrigin.LOGGER.info("✅ 已向玩家 {} 同步 {} 个其他尸兄玩家的数据",
                joiningPlayer.getName().getString(), count);
    }

    // ==================== ✅ 玩家尸兄数据同步 ====================

    public static void sendPlayerCorpseSync(ServerPlayer player) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        CompoundTag data = comp.getDataPublic();

        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                player.getUUID(),
                comp.isCorpse(),
                comp.getCorpseType(),
                data
        );

        ServerPlayNetworking.send(player, packet);
        CorpseOrigin.LOGGER.debug("同步玩家尸兄数据: {}", player.getName().getString());
    }

    public static void broadcastPlayerCorpseSync(ServerPlayer player) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        CompoundTag data = comp.getDataPublic();

        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                player.getUUID(),
                comp.isCorpse(),
                comp.getCorpseType(),
                data
        );

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, packet);
            }
            CorpseOrigin.LOGGER.debug("广播玩家尸兄数据: {} 给 {} 个玩家",
                    player.getName().getString(),
                    server.getPlayerList().getPlayers().size());
        } else {
            ServerPlayNetworking.send(player, packet);
        }
    }

    public static void sendCharacterSync(ServerPlayer player, String characterId) {
        ServerPlayNetworking.send(player, new CorpsePayloads.CharacterSyncS2C(characterId));
    }
}