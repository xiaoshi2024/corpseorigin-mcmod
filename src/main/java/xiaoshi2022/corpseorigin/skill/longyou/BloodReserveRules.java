package xiaoshi2022.corpseorigin.skill.longyou;

/** Shared eligibility for the HUD and server-side blood collection. */
public final class BloodReserveRules {
    public static final int MIN_LEVEL = 9;

    private BloodReserveRules() {}

    public static boolean eligible(String role, boolean corpse, int level) {
        return "longyou".equals(role) || "zuohufa".equals(role) || "shichaozhizi".equals(role)
                || corpse && ("corpse_brother".equals(role) || level >= MIN_LEVEL);
    }
}
