package xiaoshi2022.corpseorigin.network;
import net.minecraft.network.chat.Component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

public record SynchronizationResponsePacket(
        boolean success,
        /** 是否要播意识转移过场：只有真正的转移握手才为 true，纯提示（如死亡自动夺舍）为 false */
        boolean cameraCutscene,
        /** 播哪一套过场 */
        CameraStyle cameraStyle,
        Component message,
        UUID targetStateUuid,
        Identifier fromWorld,
        BlockPos fromPos,
        Direction fromFacing,
        Identifier toWorld,
        BlockPos toPos,
        Direction toFacing
) implements CustomPacketPayload {

    /** 意识转移过场的分支 */
    public enum CameraStyle {
        /** 天梯：先退到身后，再飞上高空，落位后从天上扎进新身体（克隆仓面板 / 死亡夺舍） */
        STAIRWAY,
        /** 直角直出：原地垂直抬起，到位后 90° 横向甩出（尸王换身），全程黑场 */
        RIGHT_ANGLE
    }

    public static SynchronizationResponsePacket failure(Component message) {
        return new SynchronizationResponsePacket(
                false, false, CameraStyle.STAIRWAY, message, new UUID(0L, 0L),
                Identifier.fromNamespaceAndPath("minecraft", "overworld"), BlockPos.ZERO, Direction.NORTH,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"), BlockPos.ZERO, Direction.NORTH);
    }

    /** 只发一条提示、不触发过场相机 */
    public static SynchronizationResponsePacket message(boolean success, Component message) {
        return new SynchronizationResponsePacket(
                success, false, CameraStyle.STAIRWAY, message,
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

    public static final StreamCodec<RegistryFriendlyByteBuf, SynchronizationResponsePacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeBoolean(p.success());
                        buf.writeBoolean(p.cameraCutscene());
                        buf.writeVarInt(p.cameraStyle().ordinal());
                        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buf,p.message());
                        buf.writeUUID(p.targetStateUuid());
                        buf.writeIdentifier(p.fromWorld());
                        buf.writeBlockPos(p.fromPos());
                        buf.writeVarInt(p.fromFacing().ordinal());
                        buf.writeIdentifier(p.toWorld());
                        buf.writeBlockPos(p.toPos());
                        buf.writeVarInt(p.toFacing().ordinal());
                    },
                    buf -> new SynchronizationResponsePacket(
                            buf.readBoolean(), buf.readBoolean(), CameraStyle.values()[buf.readVarInt()],
                            net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buf), buf.readUUID(),
                            buf.readIdentifier(), buf.readBlockPos(), Direction.values()[buf.readVarInt()],
                            buf.readIdentifier(), buf.readBlockPos(), Direction.values()[buf.readVarInt()]));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
