package xiaoshi2022.corpseorigin.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

public record SynchronizationResponsePacket(
        boolean success,
        /** 是否要播意识转移过场：只有真正的转移握手才为 true，纯提示（如死亡自动夺舍）为 false */
        boolean cameraCutscene,
        String message,
        UUID targetStateUuid,
        Identifier fromWorld,
        BlockPos fromPos,
        Direction fromFacing,
        Identifier toWorld,
        BlockPos toPos,
        Direction toFacing
) implements CustomPacketPayload {

    public static SynchronizationResponsePacket failure(String message) {
        return new SynchronizationResponsePacket(
                false, false, message, new UUID(0L, 0L),
                Identifier.fromNamespaceAndPath("minecraft", "overworld"), BlockPos.ZERO, Direction.NORTH,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"), BlockPos.ZERO, Direction.NORTH);
    }

    /** 只发一条提示、不触发过场相机 */
    public static SynchronizationResponsePacket message(boolean success, String message) {
        return new SynchronizationResponsePacket(
                success, false, message,
                new java.util.UUID(0L, 0L),
                net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.Direction.NORTH,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.Direction.NORTH);
    }

    public static final Type<SynchronizationResponsePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "sync_response"));

    public static final StreamCodec<FriendlyByteBuf, SynchronizationResponsePacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeBoolean(p.success());
                        buf.writeBoolean(p.cameraCutscene());
                        buf.writeUtf(p.message());
                        buf.writeUUID(p.targetStateUuid());
                        buf.writeIdentifier(p.fromWorld());
                        buf.writeBlockPos(p.fromPos());
                        buf.writeVarInt(p.fromFacing().ordinal());
                        buf.writeIdentifier(p.toWorld());
                        buf.writeBlockPos(p.toPos());
                        buf.writeVarInt(p.toFacing().ordinal());
                    },
                    buf -> new SynchronizationResponsePacket(
                            buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readUUID(),
                            buf.readIdentifier(), buf.readBlockPos(), Direction.values()[buf.readVarInt()],
                            buf.readIdentifier(), buf.readBlockPos(), Direction.values()[buf.readVarInt()]));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}