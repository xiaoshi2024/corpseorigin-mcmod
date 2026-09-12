// BloodLotusAuraPayload.java
package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.UUID;

/**
 * 血莲宝灯·血气爆发（自身缠绕）
 */
public record BloodLotusAuraPayload(
        UUID playerUuid,
        int durationTicks
) implements CustomPacketPayload {

    public static final Type<BloodLotusAuraPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "blood_lotus_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BloodLotusAuraPayload> CODEC =
            StreamCodec.ofMember(BloodLotusAuraPayload::write, BloodLotusAuraPayload::read);

    private static BloodLotusAuraPayload read(RegistryFriendlyByteBuf buf) {
        return new BloodLotusAuraPayload(buf.readUUID(), buf.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeVarInt(durationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}