package xiaoshi2022.corpseorigin.client.render.laser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import xiaoshi2022.corpseorigin.network.TianGangBeamPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TianGangBeamState {
    private record Entry(TianGangBeamPayload data, long expires) {}
    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();
    private static ClientLevel world;
    private static long lastPoseTick = Long.MIN_VALUE;
    public static void tick() {
        var current = Minecraft.getInstance().level;
        if (current != world) { ACTIVE.clear(); world = current; lastPoseTick = Long.MIN_VALUE; }
        if (world != null) ACTIVE.values().removeIf(e -> e.expires <= world.getGameTime());
    }
    public static void accept(TianGangBeamPayload packet) {
        tick();
        if (world == null || packet.ticks() <= 0) { ACTIVE.remove(packet.owner()); return; }
        ACTIVE.put(packet.owner(), new Entry(packet, world.getGameTime() + Math.min(10, packet.ticks())));
    }
    public static TianGangBeamPayload get(UUID owner) {
        tick();
        var entry = ACTIVE.get(owner);
        return entry == null ? null : entry.data;
    }

    /** Send only the local player's visible hand pose, at most once per game tick. */
    public static void submitPose(TianGangBeamPayload beam, net.minecraft.world.phys.Vec3 origin,
                                   net.minecraft.world.phys.Vec3 direction, boolean firstPerson) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !beam.owner().equals(mc.player.getUUID())
                || mc.getCameraEntity() != mc.player || mc.options.getCameraType().isFirstPerson() != firstPerson
                || !(mc.player.getItemInHand(beam.hand()).getItem()
                    instanceof xiaoshi2022.corpseorigin.item.weapon.TianGangKeyItem)
                || lastPoseTick == mc.level.getGameTime()) return;
        var camera = mc.gameRenderer.mainCamera();
        if (firstPerson) {
            var root = new org.joml.Vector3f((float) origin.x, (float) origin.y, (float) origin.z).rotate(camera.rotation());
            var axis = new org.joml.Vector3f((float) direction.x, (float) direction.y, (float) direction.z).rotate(camera.rotation());
            origin = new net.minecraft.world.phys.Vec3(root.x, root.y, root.z);
            direction = new net.minecraft.world.phys.Vec3(axis.x, axis.y, axis.z);
        }
        // Both render passes now use camera-relative world axes.
        var offset = origin.add(camera.position()).subtract(mc.player.position());
        direction = direction.normalize();
        if (!xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangBladeSweep.valid(offset, direction)) return;
        lastPoseTick = mc.level.getGameTime();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new xiaoshi2022.corpseorigin.network.TianGangBladePosePayload(beam.cast(), offset, direction, firstPerson));
    }
}
