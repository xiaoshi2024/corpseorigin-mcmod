package xiaoshi2022.corpseorigin.growth;

/** Shared, side-effect-free vampire identity and sunlight rules. */
public final class VampireRules {
    private VampireRules() {}

    public static boolean isVampire(String role, boolean organUnlocked) {
        return "k".equals(role) || "heixiaofei".equals(role) || organUnlocked;
    }

    public static boolean sunlightHurts(String role, boolean enabled, boolean vulnerable,
                                       boolean skylight, boolean daytime, boolean exposed,
                                       boolean raining, boolean submerged) {
        // Only K's natural vampire body has this weakness. Hei Xiaofei is immune.
        return "k".equals(role) && enabled && vulnerable && skylight && daytime
                && exposed && !raining && !submerged;
    }
}
