package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 尸王控制状态同步包 - 用于通知客户端控制状态的变化
 */
public record CorpseKingSyncPacket(
        int controlledEntityId,
        boolean isControlled,
        long controlEndTime
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "corpse_king_sync");

    public static final Type<CorpseKingSyncPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, CorpseKingSyncPacket> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT,
            CorpseKingSyncPacket::controlledEntityId,
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            CorpseKingSyncPacket::isControlled,
            net.minecraft.network.codec.ByteBufCodecs.VAR_LONG,
            CorpseKingSyncPacket::controlEndTime,
            CorpseKingSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理
     */
    public static void handleClient(CorpseKingSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端可以在这里处理控制状态的UI显示
            CorpseOrigin.LOGGER.debug("收到控制状态同步: 实体ID={}, 被控制={}, 结束时间={}",
                    packet.controlledEntityId(), packet.isControlled(), packet.controlEndTime());
        });
    }
}
