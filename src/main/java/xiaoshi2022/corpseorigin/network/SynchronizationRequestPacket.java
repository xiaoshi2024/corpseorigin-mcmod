package xiaoshi2022.corpseorigin.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

/**
 * 客户端请求转移意识。
 * <p>
 * 同时带目标坐标和目标 UUID：服务端优先按坐标解析（客户端列表可能过期，
 * 坐标才是"玩家看到的那个身体"），UUID 作为兜底。
 */
public record SynchronizationRequestPacket(UUID targetStateUuid, BlockPos targetPos)
        implements CustomPacketPayload {

    public static final Type<SynchronizationRequestPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "sync_request"));

    public static final StreamCodec<FriendlyByteBuf, SynchronizationRequestPacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeUUID(p.targetStateUuid());
                        buf.writeBlockPos(p.targetPos());
                    },
                    buf -> new SynchronizationRequestPacket(buf.readUUID(), buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}