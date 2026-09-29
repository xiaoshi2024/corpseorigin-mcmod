package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.chapter.GroundShockwave;
import xiaoshi2022.corpseorigin.skill.chapter.GroundShockwaveMath;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Measures actual descent, because flight and skills reset vanilla fallDistance. */
public final class ShenLanding {
    private ShenLanding() {}
    private record Fall(ServerPlayer player, Object level, Vec3 previous, double descent, double speed, int cooldown) {}
    private static final Map<UUID, Fall> FALLS = new HashMap<>();

    public static void register() {
        GroundShockwave.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FALLS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FALLS.clear());
    }

    private static void tick(ServerPlayer p) {
        boolean wingFlight = OrganEnergy.ownsFlight(p) && p.getAbilities().flying;
        boolean heavenly = EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID())) >= 9;
        if ((!ShenFlight.isAllowed(p) && !wingFlight) || p.isPassenger() || p.isInWater() || p.isInLava()
                || p.onClimbable() || p.hasEffect(MobEffects.SLOW_FALLING) || p.isFallFlying()) {
            FALLS.remove(p.getUUID()); return;
        }
        Fall old = FALLS.get(p.getUUID());
        Vec3 now = p.position();
        if (old == null || old.player != p || old.level != p.level() || old.previous.distanceToSqr(now) > 256) {
            FALLS.put(p.getUUID(), new Fall(p, p.level(), now, 0, 0, 0)); return;
        }
        int cooldown = Math.max(0, old.cooldown - 1);
        double down = old.previous.y - now.y;
        double descent = down < -.05 ? 0 : old.descent + Math.max(0, down);
        double speed = down < -.05 ? 0 : Math.max(old.speed, down);
        if (p.onGround()) {
            if (wingFlight && !heavenly && descent > 3 && speed > .08) {
                p.hurtServer(p.level(), p.damageSources().fall(), (float)Math.ceil(descent - 3));
            }
            if (GroundShockwaveMath.shouldTrigger(descent, speed, cooldown)) {
                GroundShockwave.spawn(p, now, Math.min(12, 4 + descent * .3), Math.min(3, 1 + descent / 20));
                cooldown = 30;
            }
            descent = 0; speed = 0;
        }
        FALLS.put(p.getUUID(), new Fall(p, p.level(), now, descent, speed, cooldown));
    }
}
