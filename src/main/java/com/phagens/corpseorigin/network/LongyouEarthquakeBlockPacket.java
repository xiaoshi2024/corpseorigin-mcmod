package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.skills.LongyouEarthquakeEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LongyouEarthquakeBlockPacket(
        int entityId,
        BlockPos pos,
        ResourceLocation blockId,  // 只传递方块ID
        double dirX, double dirY, double dirZ
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "longyou_earthquake_block");

    public static final Type<LongyouEarthquakeBlockPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, LongyouEarthquakeBlockPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            LongyouEarthquakeBlockPacket::entityId,
            BlockPos.STREAM_CODEC,
            LongyouEarthquakeBlockPacket::pos,
            ResourceLocation.STREAM_CODEC,
            LongyouEarthquakeBlockPacket::blockId,
            ByteBufCodecs.DOUBLE,
            LongyouEarthquakeBlockPacket::dirX,
            ByteBufCodecs.DOUBLE,
            LongyouEarthquakeBlockPacket::dirY,
            ByteBufCodecs.DOUBLE,
            LongyouEarthquakeBlockPacket::dirZ,
            LongyouEarthquakeBlockPacket::new
    );

    // 便捷构造方法 - 从 BlockState 创建
    public LongyouEarthquakeBlockPacket(int entityId, BlockPos pos, BlockState state, Vec3 direction) {
        this(entityId, pos, BuiltInRegistries.BLOCK.getKey(state.getBlock()), direction.x, direction.y, direction.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Vec3 getDirection() {
        return new Vec3(dirX, dirY, dirZ);
    }

    public BlockState getBlockState() {
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        return block.defaultBlockState();
    }

    public static void handleClient(LongyouEarthquakeBlockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) return;

            var level = player.level();
            var entity = level.getEntity(packet.entityId());

            if (entity instanceof LongyouEarthquakeEntity earthquake) {
                earthquake.addBlockInstance(packet.pos(), packet.getBlockState(), packet.getDirection());
//                CorpseOrigin.LOGGER.debug("客户端收到地震方块翻动包: 实体ID={}, 位置={}, 方块={}",
//                        packet.entityId(), packet.pos(), packet.blockId());
            }
        });
    }
}