package com.phagens.corpseorigin.GongFU.PackGongFu;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.PackGongFu.Paket.OpenGongFuMenuPacket;
import com.phagens.corpseorigin.GongFU.Sceen.GongFuMenu;

import com.phagens.corpseorigin.network.*;
import com.phagens.corpseorigin.network.TechniqueSwapCraftPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

//网络管理
public class NetworkPaketGL {

    // 公共静态注册方法
    public static void registerPackets(IEventBus modEventBus) {
        modEventBus.addListener(NetworkPaketGL::registerNetworkPackets);
    }

    public static void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        // 修复：使用与 NetworkRegister 相同的 channel 名称
        // 确保所有包都在同一个通道下注册
        PayloadRegistrar registrar = event.registrar(CorpseOrigin.MODID)  // 改为使用 MODID
                .versioned("1.0.0");

        CorpseOrigin.LOGGER.info("NetworkPaketGL 正在注册网络包到通道: {}", CorpseOrigin.MODID);

        // 注册双向包（客户端发送，服务端接收）
        registrar.playBidirectional(
                OpenGongFuMenuPacket.TYPE,
                OpenGongFuMenuPacket.STREAM_CODEC,
                (packet, context) -> {
                    if (!context.flow().isClientbound()) {
                        // 服务端处理
                        handleOpenGongFuMenu(packet, context);
                    }
                    // 客户端不处理这个包
                }
        );

        // 注册技能同步包（服务器到客户端）
        registrar.playToClient(
                SyncSkillDataPacket.TYPE,
                SyncSkillDataPacket.STREAM_CODEC,
                SyncSkillDataPacket::handleClient
        );

        // 注册解锁技能包（客户端到服务器）- 使用旧的包
        registrar.playToServer(
                UnlockSkillPacket.TYPE,
                UnlockSkillPacket.STREAM_CODEC,
                UnlockSkillPacket::handleServer
        );

        // 注册激活技能包（客户端到服务器）
        registrar.playToServer(
                ActivateSkillPacket.TYPE,
                ActivateSkillPacket.STREAM_CODEC,
                ActivateSkillPacket::handleServer
        );

        // 注册新的技能解锁包
        registrar.playToServer(
                SkillUnlockPacket.TYPE,
                SkillUnlockPacket.STREAM_CODEC,
                SkillUnlockPacket::handleOnServer
        );

        // 注册经验转化包
        registrar.playToServer(
                com.phagens.corpseorigin.network.ExperienceConvertPacket.TYPE,
                com.phagens.corpseorigin.network.ExperienceConvertPacket.STREAM_CODEC,
                com.phagens.corpseorigin.network.ExperienceConvertPacket::handleOnServer
        );

        // 注册巨阙剑气包（客户端到服务器）
        registrar.playToServer(
                com.phagens.corpseorigin.network.JuQueBeamPacket.TYPE,
                com.phagens.corpseorigin.network.JuQueBeamPacket.STREAM_CODEC,
                com.phagens.corpseorigin.network.JuQueBeamPacket::handle
        );

        registrar.playToServer(
                com.phagens.corpseorigin.network.TechniqueSwapCraftPacket.TYPE,
                com.phagens.corpseorigin.network.TechniqueSwapCraftPacket.STREAM_CODEC,
                com.phagens.corpseorigin.network.TechniqueSwapCraftPacket::handle
        );
    }

    private static void handleOpenGongFuMenu(OpenGongFuMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, player) -> new GongFuMenu(containerId, inventory),
                        Component.translatable("修行")
                ));
            }
        });
    }
}