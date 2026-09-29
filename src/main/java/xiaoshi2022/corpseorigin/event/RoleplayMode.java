package xiaoshi2022.corpseorigin.event;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/** World-wide filming mode, persisted in the overworld save. */
public final class RoleplayMode extends SavedData {
    private static final Codec<RoleplayMode> CODEC = Codec.BOOL.xmap(RoleplayMode::new, mode -> mode.enabled);
    private static final SavedDataType<RoleplayMode> TYPE = new SavedDataType<>(
            CorpseOrigin.id("roleplay_mode"), RoleplayMode::new, CODEC, null);
    private boolean enabled;

    private RoleplayMode() {}
    private RoleplayMode(boolean enabled) { this.enabled = enabled; }

    private static RoleplayMode get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean isEnabled(MinecraftServer server) {
        return get(server).enabled;
    }

    public static void setEnabled(MinecraftServer server, boolean enabled) {
        RoleplayMode mode = get(server);
        if (mode.enabled != enabled) {
            mode.enabled = enabled;
            mode.setDirty();
        }
    }
}
