package com.xiaoshi2022.corpseorigin.network;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.growth.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.function.Supplier;

/** All mutations to live entities happen after server-side range and permission checks. */
@Mod.EventBusSubscriber(modid = CorpseOrigin.MODID)
public final class OrganNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(CorpseOrigin.MODID, "organs"), () -> "1", "1"::equals, "1"::equals);
    private static final Map<UUID, Long> RELOADS = new HashMap<>();
    private OrganNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER).encoder(Request::encode).decoder(Request::decode).consumerMainThread(OrganNetwork::handle).add();
        CHANNEL.messageBuilder(Reply.class, 1, NetworkDirection.PLAY_TO_CLIENT).encoder(Reply::encode).decoder(Reply::decode).consumerMainThread((m,c) -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.xiaoshi2022.corpseorigin.client.OrganClient.accept(m)); c.get().setPacketHandled(true);
        }).add();
        CHANNEL.messageBuilder(State.class, 2, NetworkDirection.PLAY_TO_CLIENT).encoder(State::encode).decoder(State::decode).consumerMainThread((m,c) -> {
            NativeCorpseState.acceptClientOrgans(m.uuid, m.json);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.xiaoshi2022.corpseorigin.client.OrganClient.state(m.uuid));
            c.get().setPacketHandled(true);
        }).add();
    }
    public record Request(String action, int target, String json) {
        void encode(FriendlyByteBuf b) { b.writeUtf(action, 16); b.writeInt(target); b.writeUtf(json, 8192); }
        static Request decode(FriendlyByteBuf b) { return new Request(b.readUtf(16), b.readInt(), b.readUtf(8192)); }
    }
    public record Reply(boolean success, Component status, String catalog) {
        void encode(FriendlyByteBuf b) { b.writeBoolean(success); b.writeComponent(status); b.writeUtf(catalog, 262144); }
        static Reply decode(FriendlyByteBuf b) { return new Reply(b.readBoolean(), b.readComponent(), b.readUtf(262144)); }
    }
    public record State(UUID uuid, String json) {
        void encode(FriendlyByteBuf b) { b.writeUUID(uuid); b.writeUtf(json, 8192); }
        static State decode(FriendlyByteBuf b) { return new State(b.readUUID(), b.readUtf(8192)); }
    }
    public static void request(String action, int target, String json) { CHANNEL.sendToServer(new Request(action, target, json)); }
    private static void reply(ServerPlayer p, boolean ok, Component message, boolean catalog) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Reply(ok, message,
                catalog ? OrganLibrary.JSON.toJson(OrganLibrary.definitions()) : ""));
    }
    public static void sync(LivingEntity entity) {
        if (!entity.level().isClientSide()) CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new State(entity.getUUID(), NativeCorpseState.organLoadout(entity)));
    }
    public static void syncTo(ServerPlayer p, LivingEntity entity) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new State(entity.getUUID(), NativeCorpseState.organLoadout(entity)));
    }
    private static LivingEntity editable(ServerPlayer p, int id) {
        if (id < 0 || id == p.getId()) return p;
        Entity target = p.level().getEntity(id);
        if (!(target instanceof Mob mob) || !mob.isAlive() || !NativeCorpseState.infected(mob)
                || p.distanceToSqr(mob) > 64 || !p.hasLineOfSight(mob)) throw OrganText.error("look_target");
        if (!p.isCreative() && !p.hasPermissions(2)) throw OrganText.error("edit_permission");
        return mob;
    }
    private static void handle(Request m, Supplier<NetworkEvent.Context> context) {
        ServerPlayer p = context.get().getSender();
        try {
            if (p == null || !p.isAlive() || p.isSpectator()) return;
            switch (m.action) {
                case "catalog", "refresh" -> {
                    if (m.action.equals("refresh")) {
                        long now = System.currentTimeMillis();
                        if (now - RELOADS.getOrDefault(p.getUUID(), 0L) < 5000) { reply(p, false, OrganText.text("wait"), false); break; }
                        if (p.getServer().isDedicatedServer() && !p.hasPermissions(2)) { reply(p, false, OrganText.text("reload_permission"), true); break; }
                        RELOADS.put(p.getUUID(), now); OrganLibrary.load();
                        for (ServerPlayer other : p.getServer().getPlayerList().getPlayers()) reply(other, true, OrganLibrary.packStatus, true);
                    } else reply(p, true, OrganLibrary.packStatus, true);
                    syncTo(p, editable(p, m.target));
                }
                case "save", "summon" -> {
                    var slots = OrganLibrary.parseSlots(m.json);
                    for (var slot : slots) if (OrganLibrary.find(slot.organ()) == null) throw OrganText.error("unknown_organ",slot.organ());
                    String json = OrganLibrary.toJson(slots);
                    if (m.action.equals("summon")) {
                        if (!p.isCreative() && !p.hasPermissions(2)) throw OrganText.error("summon_permission");
                        var mob = EntityType.ZOMBIE.create(p.serverLevel());
                        if (mob == null) throw OrganText.error("create_failed");
                        var pos = p.position().add(p.getLookAngle().multiply(2, 0, 2));
                        mob.moveTo(pos.x, p.getY(), pos.z, p.getYRot(), 0);
                        if (!p.serverLevel().noCollision(mob)) throw OrganText.error("space");
                        NativeCorpseState.transform(mob, p.serverLevel()); NativeCorpseState.setOrganLoadout(mob, json);
                        mob.setCustomName(OrganText.text("corpse_name",p.getGameProfile().getName())); mob.setPersistenceRequired();
                        if (!p.serverLevel().addFreshEntity(mob)) throw OrganText.error("spawn_failed");
                        sync(mob); CorpseOriginNetwork.sync(mob);
                        reply(p, true, OrganText.text("spawned"), false);
                    } else {
                        LivingEntity target = editable(p, m.target);
                        NativeCorpseState.setOrganLoadout(target, json); OrganTraits.update(target); sync(target);
                        reply(p, true, OrganText.text("saved"), false);
                    }
                }
                default -> throw OrganText.error("unknown_action");
            }
        } catch (Exception e) { if (p != null) reply(p, false, OrganText.failure(e), false); }
        finally { context.get().setPacketHandled(true); }
    }
    @SubscribeEvent public static void started(ServerStartedEvent e) { RELOADS.clear(); OrganLibrary.load(); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { reply(p, true, OrganLibrary.packStatus, true); sync(p); }
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof LivingEntity living) syncTo(p, living);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        NativeCorpseState.setOrganLoadout(e.getEntity(), NativeCorpseState.organLoadout(e.getOriginal()));
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { RELOADS.remove(e.getEntity().getUUID()); }
}
