package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.client.skin.ZbSkinState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZbSkinUpdatePacket(int entityId, ResourceLocation skinTexture, int skinStateCode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ZbSkinUpdatePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "skin_update"));

    public static final StreamCodec<FriendlyByteBuf, ZbSkinUpdatePacket> STREAM_CODEC =
            StreamCodec.ofMember(ZbSkinUpdatePacket::write, ZbSkinUpdatePacket::new);

    public ZbSkinUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readInt(),
                buf.readBoolean() ? buf.readResourceLocation() : null,
                buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(skinTexture != null);
        if (skinTexture != null) {
            buf.writeResourceLocation(skinTexture);
        }
        buf.writeInt(skinStateCode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final ZbSkinUpdatePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) return;

            ServerLevel level = (ServerLevel) player.level();
            Entity entity = level.getEntity(data.entityId());

            if (entity instanceof LowerLevelZbEntity zbEntity) {
                if (data.skinTexture() != null) {
                    zbEntity.setSkinTextureFromServer(data.skinTexture());
                }
                zbEntity.setSkinStateFromServer(ZbSkinState.fromCode(data.skinStateCode()));
//                CorpseOrigin.LOGGER.debug("服务端收到皮肤更新: 实体 {} 状态 {}", data.entityId(), data.skinStateCode());
            }
        });
    }
}