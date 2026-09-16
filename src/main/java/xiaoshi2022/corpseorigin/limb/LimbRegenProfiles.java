package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 再生策略注册表：按优先级取第一条 {@link LimbRegenProfile#appliesTo} 命中的策略。
 * <p>
 * 列表为空时兜底到 {@link CorpseRegenProfile}，保证任何情况下都有策略可用。
 */
public final class LimbRegenProfiles {

    private static final List<LimbRegenProfile> PROFILES = new ArrayList<>();
    private static final LimbRegenProfile FALLBACK = new CorpseRegenProfile();
    private static boolean defaultsRegistered;

    private LimbRegenProfiles() {
    }

    public static void register(LimbRegenProfile profile) {
        PROFILES.removeIf(existing -> existing.id().equals(profile.id()));
        PROFILES.add(profile);
        PROFILES.sort(Comparator.comparingInt(LimbRegenProfile::priority));
    }

    public static void unregister(String id) {
        PROFILES.removeIf(profile -> profile.id().equals(id));
    }

    public static List<LimbRegenProfile> all() {
        return Collections.unmodifiableList(PROFILES);
    }

    public static LimbRegenProfile forPlayer(ServerPlayer player) {
        for (LimbRegenProfile profile : PROFILES) {
            if (profile.appliesTo(player)) {
                return profile;
            }
        }
        return FALLBACK;
    }

    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        defaultsRegistered = true;

        register(new VampireRegenProfile());
        register(new CorpseRegenProfile());
    }
}
