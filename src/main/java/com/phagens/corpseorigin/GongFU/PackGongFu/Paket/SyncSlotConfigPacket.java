package com.phagens.corpseorigin.GongFU.PackGongFu.Paket;

import com.phagens.corpseorigin.CorpseOrigin;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncSlotConfigPacket(CompoundTag slotData) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "sync_slot_config");

    public static final Type<SyncSlotConfigPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, SyncSlotConfigPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            SyncSlotConfigPacket::slotData,
            SyncSlotConfigPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static SyncSlotConfigPacket create(Player player) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag slotData = playerData.getCompound("GongFuSlotData").copy();
        return new SyncSlotConfigPacket(slotData);
    }

    public static void handleClient(SyncSlotConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                CompoundTag playerData = player.getPersistentData();
                playerData.put("GongFuSlotData", packet.slotData());
                
                CorpseOrigin.LOGGER.debug("【客户端】槽位配置已同步");
            }
        });
    }
}
