package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

/**
 * 血莲宝灯·雷电链条效果包
 * <p>
 * 服务端发射，广播给附近玩家渲染。
 * 携带目标 UUID，客户端每 tick 追踪目标位置。
 */
public record BloodLotusLaserPayload(
        double startX, double startY, double startZ,
        UUID targetUuid,
        int durationTicks
) implements CustomPacketPayload {

    public static final Type<BloodLotusLaserPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "blood_lotus_laser"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BloodLotusLaserPayload> CODEC =
            StreamCodec.ofMember(BloodLotusLaserPayload::write, BloodLotusLaserPayload::read);

    private static BloodLotusLaserPayload read(RegistryFriendlyByteBuf buf) {
        return new BloodLotusLaserPayload(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readUUID(),
                buf.readInt()
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(startX);
        buf.writeDouble(startY);
        buf.writeDouble(startZ);
        buf.writeUUID(targetUuid);
        buf.writeInt(durationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Vec3 getStart() {
        return new Vec3(startX, startY, startZ);
    }

    public static BloodLotusLaserPayload create(Vec3 start, UUID targetUuid, int durationTicks) {
        return new BloodLotusLaserPayload(
                start.x, start.y, start.z,
                targetUuid, durationTicks
        );
    }
}