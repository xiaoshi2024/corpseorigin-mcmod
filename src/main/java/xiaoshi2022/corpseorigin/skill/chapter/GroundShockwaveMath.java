package xiaoshi2022.corpseorigin.skill.chapter;

/** Pure animation/landing rules, also usable by previews without loading a world. */
public final class GroundShockwaveMath {
    private GroundShockwaveMath() {}
    public static final int LIFE = 22;
    public static boolean shouldTrigger(double descent, double speed, int cooldown) {
        return Double.isFinite(descent) && Double.isFinite(speed) && cooldown == 0 && descent >= 4 && speed >= .45;
    }
    public static float envelope(float age) {
        if (!Float.isFinite(age) || age <= 0 || age >= LIFE) return 0;
        if (age < 6) { float t = age / 6; return 1 - (1 - t) * (1 - t); }
        if (age <= 10) return 1;
        float t = (age - 10) / (LIFE - 10);
        return 1 - t * t;
    }
}
