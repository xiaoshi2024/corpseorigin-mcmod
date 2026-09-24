package xiaoshi2022.corpseorigin.growth;

/** Deterministic progression; creative bypasses gameplay gates, not numeric validation. */
public final class OrganEvolutionRules {
    private OrganEvolutionRules() {}
    public static int cost(int next) { return next == 1 ? 5 : next == 2 ? 10 : 20; }
    public static int level(int next) { return next == 1 ? 1 : next == 2 ? 5 : 9; }
    public static int attributeCost(int current) { return 2 + Math.min(1000, current); }
    public static boolean mayAdvance(boolean creative, int stage, int level, int points) {
        return stage >= 0 && stage < 3 && (creative || level >= level(stage+1) && points >= cost(stage+1));
    }
    public static boolean mayAllocate(boolean creative, int stage, int current, int points) {
        return current >= 0 && current < 1000000 && (creative || stage >= 2 && current < 5 && points >= attributeCost(current));
    }
    public static String stageName(int stage) {
        return switch(stage) { case 0 -> "organ.corpseorigin.stage.0"; case 1 -> "organ.corpseorigin.stage.1"; case 2 -> "organ.corpseorigin.stage.2"; default -> "organ.corpseorigin.stage.3"; };
    }
}
