package xiaoshi2022.corpseorigin.skill.longyou;

/** Pure rules for the shared, rate-limited raw-meat reserve. */
public final class RawMeatDigestionRules {
    public static final int BLOOD_PER_MEAT = 5;
    public static final int MAX_PENDING = 20;
    public static final int TICKS_PER_POINT = 40;

    private RawMeatDigestionRules() {}

    public static int accepted(int blood, int maximum, int pending) {
        return Math.max(0, Math.min(BLOOD_PER_MEAT,
                Math.min(MAX_PENDING - pending, maximum - blood - pending)));
    }

    public record Step(int pending, int ticks, int gained) {}

    public static Step tick(int pending, int ticks, boolean active, boolean full) {
        if (!active || full || pending <= 0) return new Step(0, 0, 0);
        if (ticks + 1 < TICKS_PER_POINT) return new Step(pending, ticks + 1, 0);
        return new Step(pending - 1, 0, 1);
    }
}
