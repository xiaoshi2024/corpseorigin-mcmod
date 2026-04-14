package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.player.CorpsePlayerAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerCorpseSyncPacket(int playerId, boolean isCorpse, int corpseType, CompoundTag corpseData) implements CustomPacketPayload {
    public static final Type<PlayerCorpseSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "player_corpse_sync"));

    public static final StreamCodec<FriendlyByteBuf, PlayerCorpseSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PlayerCorpseSyncPacket::playerId,
            ByteBufCodecs.BOOL,
            PlayerCorpseSyncPacket::isCorpse,
            ByteBufCodecs.INT,
            PlayerCorpseSyncPacket::corpseType,
            ByteBufCodecs.COMPOUND_TAG,
            PlayerCorpseSyncPacket::corpseData,
            PlayerCorpseSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerCorpseSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // 尝试直接获取玩家实体
                Player player = mc.level.getEntity(packet.playerId) instanceof Player p ? p : null;
                if (player != null) {
                    // 直接更新状态
                    player.setData(CorpsePlayerAttachment.IS_CORPSE, packet.isCorpse);
                    player.setData(CorpsePlayerAttachment.CORPSE_TYPE, packet.corpseType);
                    player.setData(CorpsePlayerAttachment.CORPSE_DATA, packet.corpseData);
                } else {
                    // 如果玩家实体还未加载，延迟处理
                    mc.execute(() -> {
                        Player delayedPlayer = mc.level.getEntity(packet.playerId) instanceof Player p ? p : null;
                        if (delayedPlayer != null) {
                            delayedPlayer.setData(CorpsePlayerAttachment.IS_CORPSE, packet.isCorpse);
                            delayedPlayer.setData(CorpsePlayerAttachment.CORPSE_TYPE, packet.corpseType);
                            delayedPlayer.setData(CorpsePlayerAttachment.CORPSE_DATA, packet.corpseData);
                        }
                    });
                }
            }
        });
    }
}
