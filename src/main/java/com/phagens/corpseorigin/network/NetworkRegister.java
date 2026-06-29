package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CorpseOrigin.MODID )
public class NetworkRegister {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CorpseOrigin.MODID)
                .versioned("1.0.0");

        CorpseOrigin.LOGGER.info("NetworkRegister 正在注册网络包到通道: {}", CorpseOrigin.MODID);

        // 注册皮肤更新包 (客户端 → 服务端)
        registrar.playToServer(
                ZbSkinUpdatePacket.TYPE,
                ZbSkinUpdatePacket.STREAM_CODEC,
                ZbSkinUpdatePacket::handleData
        );

        // 注册玩家尸体状态同步包 (服务端 → 客户端)
        registrar.playToClient(
                PlayerCorpseSyncPacket.TYPE,
                PlayerCorpseSyncPacket.STREAM_CODEC,
                PlayerCorpseSyncPacket::handle
        );

        // 注册尸王控制包 (客户端 → 服务端)
        registrar.playToServer(
                CorpseKingControlPacket.TYPE,
                CorpseKingControlPacket.STREAM_CODEC,
                CorpseKingControlPacket::handleServer
        );

        // 注册尸王控制状态同步包 (服务端 → 客户端)
        registrar.playToClient(
                CorpseKingSyncPacket.TYPE,
                CorpseKingSyncPacket.STREAM_CODEC,
                CorpseKingSyncPacket::handleClient
        );

        // 注册功法容器同步包 (客户端 → 服务端)
        registrar.playToServer(
                SyncGongFuContainerPacket.TYPE,
                SyncGongFuContainerPacket.STREAM_CODEC,
                SyncGongFuContainerPacket::handleServer
        );

        // ⭐ 新增：注册龙右地震方块翻动包 (服务端 → 客户端)
        registrar.playToClient(
                LongyouEarthquakeBlockPacket.TYPE,
                LongyouEarthquakeBlockPacket.STREAM_CODEC,
                LongyouEarthquakeBlockPacket::handleClient
        );

        // ⭐ 新增：注册龙右对话包 (客户端 → 服务端)
        registrar.playToServer(
                LongyouDialoguePacket.TYPE,
                LongyouDialoguePacket.STREAM_CODEC,
                LongyouDialoguePacket::handle
        );

        // ⭐ 新增：注册龙右对话选项包 (服务端 → 客户端)
        registrar.playToClient(
                LongyouDialogueOptionsPacket.TYPE,
                LongyouDialogueOptionsPacket.STREAM_CODEC,
                LongyouDialogueOptionsPacket::handle
        );

        // ⭐ 新增：注册巨阙剑取出包 (客户端 → 服务端)
        registrar.playToServer(
                JuQueTakeOutPacket.TYPE,
                JuQueTakeOutPacket.STREAM_CODEC,
                JuQueTakeOutPacket::handle
        );

        // ⭐ 新增：注册角色同步包 (服务端 → 客户端)
        registrar.playToClient(
                CharacterSyncPacket.TYPE,
                CharacterSyncPacket.STREAM_CODEC,
                CharacterSyncPacket::handle
        );

        CorpseOrigin.LOGGER.info("NetworkRegister 网络包注册完成 - 共注册了 8 个包");
    }
}