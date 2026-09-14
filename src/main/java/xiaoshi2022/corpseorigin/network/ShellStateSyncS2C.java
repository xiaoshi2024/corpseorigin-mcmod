package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 只同步渲染需要的轻量信息：uuid、owner、世界、坐标、进度。
 * 完整 body 不在这里传。
 */
public record ShellStateSyncS2C(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(UUID uuid, UUID ownerUuid, String world, int x, int y, int z, float progress) {
    }

    public static final Type<ShellStateSyncS2C> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "shell_state_sync"));

    public static final StreamCodec<FriendlyByteBuf, ShellStateSyncS2C> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeVarInt(p.entries().size());
                        for (Entry e : p.entries()) {
                            buf.writeUUID(e.uuid());
                            buf.writeUUID(e.ownerUuid());
                            buf.writeUtf(e.world());
                            buf.writeInt(e.x());
                            buf.writeInt(e.y());
                            buf.writeInt(e.z());
                            buf.writeFloat(e.progress());
                        }
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<Entry> list = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            list.add(new Entry(
                                    buf.readUUID(), buf.readUUID(), buf.readUtf(),
                                    buf.readInt(), buf.readInt(), buf.readInt(),
                                    buf.readFloat()));
                        }
                        return new ShellStateSyncS2C(list);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}