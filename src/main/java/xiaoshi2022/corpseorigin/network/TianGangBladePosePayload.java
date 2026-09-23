package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import java.util.UUID;

/** Pose input only. Clients cannot supply targets, damage, length or resource state. */
public record TianGangBladePosePayload(UUID cast, Vec3 rootOffset, Vec3 direction, boolean firstPerson)
        implements CustomPacketPayload {
    public static final Type<TianGangBladePosePayload> TYPE = new Type<>(CorpseOrigin.id("tian_gang_blade_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TianGangBladePosePayload> CODEC = StreamCodec.ofMember(
            (p, b) -> { b.writeUUID(p.cast); write(b, p.rootOffset); write(b, p.direction); b.writeBoolean(p.firstPerson); },
            b -> new TianGangBladePosePayload(b.readUUID(), read(b), read(b), b.readBoolean()));
    private static void write(RegistryFriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    private static Vec3 read(RegistryFriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
