package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 血莲宝灯·多目标链条效果包
 * <p>
 * 一个包携带所有目标 UUID，客户端一次渲染多条链条。
 */
public record BloodLotusLaserMultiPayload(
        double startX, double startY, double startZ,
        List<UUID> targetUuids,
        int durationTicks
) implements CustomPacketPayload {

    public static final Type<BloodLotusLaserMultiPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "blood_lotus_laser_multi"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BloodLotusLaserMultiPayload> CODEC =
            StreamCodec.ofMember(BloodLotusLaserMultiPayload::write, BloodLotusLaserMultiPayload::read);

    private static BloodLotusLaserMultiPayload read(RegistryFriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int count = buf.readVarInt();
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(buf.readUUID());
        }
        int duration = buf.readVarInt();
        return new BloodLotusLaserMultiPayload(x, y, z, uuids, duration);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(startX);
        buf.writeDouble(startY);
        buf.writeDouble(startZ);
        buf.writeVarInt(targetUuids.size());
        for (UUID uuid : targetUuids) {
            buf.writeUUID(uuid);
        }
        buf.writeVarInt(durationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Vec3 getStart() {
        return new Vec3(startX, startY, startZ);
    }

    public static BloodLotusLaserMultiPayload create(Vec3 start, List<UUID> targets, int durationTicks) {
        return new BloodLotusLaserMultiPayload(
                start.x, start.y, start.z,
                List.copyOf(targets), durationTicks
        );
    }
}