package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
            ClientHandler.handlePlayerCorpseSync(
                    packet.playerId(),
                    packet.isCorpse(),
                    packet.corpseType(),
                    packet.corpseData(),
                    context
            );
        });
    }
}
