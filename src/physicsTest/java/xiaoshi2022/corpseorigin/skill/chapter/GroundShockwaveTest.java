package xiaoshi2022.corpseorigin.skill.chapter;

public final class GroundShockwaveTest {
    public static void main(String[] args) {
        check(!GroundShockwaveMath.shouldTrigger(1.25, .6, 0), "ordinary jump");
        check(!GroundShockwaveMath.shouldTrigger(20, .1, 0), "gentle descent");
        check(!GroundShockwaveMath.shouldTrigger(20, 1.8, 1), "cooldown");
        check(GroundShockwaveMath.shouldTrigger(4, .45, 0), "landing threshold");
        check(GroundShockwaveMath.shouldTrigger(30, 1.8, 0), "high speed landing");
        check(!GroundShockwaveMath.shouldTrigger(Double.NaN, 1, 0), "invalid distance");
        check(!GroundShockwaveMath.shouldTrigger(5, Double.POSITIVE_INFINITY, 0), "invalid speed");
        for (int i = -10; i <= 240; i++) {
            float age = i / 10f, value = GroundShockwaveMath.envelope(age);
            check(value >= 0 && value <= 1, "bounded animation");
            if (age < 0 || age >= 22) check(value == 0, "no residual displacement");
            if (age >= 6 && age <= 10) check(value == 1, "held peak");
            if (age >= 0 && age < 6) check(value <= GroundShockwaveMath.envelope(age + .01f), "rise");
            if (age >= 10) check(value >= GroundShockwaveMath.envelope(age + .01f), "settle");
        }
        for (float boundary : new float[]{0, 6, 10, 22})
            check(Math.abs(GroundShockwaveMath.envelope(boundary - .001f)
                    - GroundShockwaveMath.envelope(boundary + .001f)) < .001f, "continuous phase boundary");
        System.out.println("GroundShockwaveTest passed");
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
