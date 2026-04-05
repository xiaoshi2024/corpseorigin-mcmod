package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LongyouDialoguePacket(String option) implements CustomPacketPayload {
    public static final Type<LongyouDialoguePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "longyou_dialogue"));

    public static final StreamCodec<FriendlyByteBuf, LongyouDialoguePacket> STREAM_CODEC = 
            StreamCodec.ofMember(LongyouDialoguePacket::write, LongyouDialoguePacket::new);

    public LongyouDialoguePacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(option);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LongyouDialoguePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 处理服务器端逻辑
            net.minecraft.server.level.ServerPlayer player = context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                // 查找附近的龙右实体
                java.util.List<com.phagens.corpseorigin.entity.LongyouEntity> longyouEntities = player.level().getEntitiesOfClass(
                    com.phagens.corpseorigin.entity.LongyouEntity.class,
                    player.getBoundingBox().inflate(10)
                );
                
                if (!longyouEntities.isEmpty()) {
                    com.phagens.corpseorigin.entity.LongyouEntity longyou = longyouEntities.get(0);
                    longyou.handleDialogueOption(player, packet.option);
                }
            }
        });
    }
}
