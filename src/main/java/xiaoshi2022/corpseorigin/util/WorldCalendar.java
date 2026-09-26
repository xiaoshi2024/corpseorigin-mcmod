package xiaoshi2022.corpseorigin.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;

/** Calendar time advances when players sleep, unlike elapsed game time. */
public final class WorldCalendar {
    private WorldCalendar() {}

    public static long dayTime(ServerLevel level) {
        var clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);
        return level.clockManager().getTotalTicks(clock);
    }
}
