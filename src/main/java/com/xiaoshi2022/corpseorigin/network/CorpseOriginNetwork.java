package com.xiaoshi2022.corpseorigin.network;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 尸变状态的客户端同步。
 * <p>
 * 1.20.1 不会把「已追踪实体新增的 effect」自动同步给客户端，
 * 而 entityTags 也完全不同步，所以这里显式发一个包把
 * 「实体 UUID + 外观掩码」告诉客户端，供渲染层使用。
 */
public final class CorpseOriginNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CorpseOrigin.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    private CorpseOriginNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(SyncInfection.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncInfection::encode)
                .decoder(SyncInfection::decode)
                .consumerMainThread(SyncInfection::handle)
                .add();
    }

    /** 广播给所有正在追踪该实体的玩家。 */
    public static void sync(Mob mob) {
        if (mob.level().isClientSide()) return;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                new SyncInfection(mob.getUUID(), NativeCorpseState.appearance(mob)));
    }

    /** 单独发给某个玩家（用于玩家开始追踪/进入世界时补发）。 */
    public static void syncTo(ServerPlayer player, Mob mob) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncInfection(mob.getUUID(), NativeCorpseState.appearance(mob)));
    }

    public record SyncInfection(UUID entityId, int mask) {

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(this.entityId);
            buffer.writeVarInt(this.mask);
        }

        public static SyncInfection decode(FriendlyByteBuf buffer) {
            return new SyncInfection(buffer.readUUID(), buffer.readVarInt());
        }

        public static void handle(SyncInfection message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> NativeCorpseState.acceptClientState(message.entityId, message.mask));
            context.get().setPacketHandled(true);
        }
    }
}
