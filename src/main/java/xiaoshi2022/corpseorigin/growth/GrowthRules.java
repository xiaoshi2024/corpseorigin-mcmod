package xiaoshi2022.corpseorigin.growth;

/** Pure progression rules, shared by configuration handling and regression checks. */
public final class GrowthRules {
    private GrowthRules() {}
    public static int progress(int current, int required) {
        return (int)Math.min(Math.max(1, required), (long)Math.max(0, current) + 1);
    }
    public static boolean inherit(double roll, double chance) {
        return Double.isFinite(chance) && roll >= 0 && roll < Math.clamp(chance, 0, 1);
    }
    public static int reward(int earned, int requested) {
        return (int)Math.max(0, Math.min((long)Math.max(0, requested), Integer.MAX_VALUE - (long)Math.max(0, earned)));
    }
}
