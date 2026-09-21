package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import java.util.ArrayList;
import java.util.List;

public record NestRadarPayload(boolean open, List<Contact> contacts) implements CustomPacketPayload {
    public record Contact(int id, String name, float x, float z, boolean alarm) {}
    public static final Type<NestRadarPayload> TYPE = new Type<>(CorpseOrigin.id("nest_radar"));
    public static final StreamCodec<FriendlyByteBuf, NestRadarPayload> CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeBoolean(p.open()); b.writeVarInt(p.contacts().size());
                for (Contact c : p.contacts()) {
                    b.writeVarInt(c.id()); b.writeUtf(c.name(), 128);
                    b.writeFloat(c.x()); b.writeFloat(c.z()); b.writeBoolean(c.alarm());
                }
            }, b -> {
                boolean open = b.readBoolean(); int count = b.readVarInt();
                if (count < 0 || count > 64) throw new IllegalArgumentException("Radar contact count");
                List<Contact> list = new ArrayList<>();
                for (int i = 0; i < count; i++) list.add(new Contact(b.readVarInt(), b.readUtf(128),
                        b.readFloat(), b.readFloat(), b.readBoolean()));
                return new NestRadarPayload(open, List.copyOf(list));
            });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Action(int target, int mode) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(CorpseOrigin.id("nest_radar_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = CustomPacketPayload.codec(
                (p, b) -> { b.writeVarInt(p.target()); b.writeVarInt(p.mode()); },
                b -> new Action(b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
