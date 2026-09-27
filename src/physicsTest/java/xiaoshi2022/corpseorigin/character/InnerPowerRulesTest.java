package xiaoshi2022.corpseorigin.character;

import xiaoshi2022.corpseorigin.skill.EvolutionManager;

public final class InnerPowerRulesTest {
    private static int checks;

    public static void main(String[] args) {
        // The actual cumulative-point boundary, including old saves already above Tian.
        equal(0, capacityAtPoints(0, 0, 199));
        equal(260, capacityAtPoints(0, 0, 200));
        equal(260, capacityAtPoints(0, 0, 279));
        equal(280, capacityAtPoints(0, 0, 280));
        equal(480, capacityAtPoints(0, 0, Integer.MAX_VALUE));

        // Native qi must never be replaced by the tier fallback, even if it is smaller.
        for (int level = 1; level <= EvolutionManager.MAX_LEVEL; level++) {
            equal(80, InnerPowerRules.capacity(80, 0, level));
            equal(600, InnerPowerRules.capacity(600, 0, level));
            equal(100, InnerPowerRules.capacity(0, 100, level));
            equal(300, InnerPowerRules.capacity(80, 300, level));
            check(!InnerPowerRules.awakensAtTier(80, 0, level));
            check(!InnerPowerRules.awakensAtTier(0, 100, level));
        }

        // Switching to a lower-level body cannot retain the previous body's tier bonus.
        equal(280, InnerPowerRules.capacity(0, 0, 10));
        equal(0, InnerPowerRules.capacity(0, 0, 8));
        equal(260, InnerPowerRules.capacity(0, 0, 9));
        check(!InnerPowerRules.awakensAtTier(0, 0, 8));
        check(InnerPowerRules.awakensAtTier(0, 0, 9));
        check(InnerPowerRules.awakensAtTier(0, 0, 20));
        equal(100, InnerPowerRules.growthCapacity(1));
        equal(260, InnerPowerRules.growthCapacity(9));
        equal(100, InnerPowerRules.growthCapacity(Integer.MIN_VALUE));
        equal(480, InnerPowerRules.growthCapacity(Integer.MAX_VALUE));
        System.out.println("InnerPowerRulesTest: " + checks + " checks passed");
    }

    private static int capacityAtPoints(int innate, int learned, int points) {
        return InnerPowerRules.capacity(innate, learned, EvolutionManager.getLevel(points));
    }

    private static void equal(int expected, int actual) {
        checks++;
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Check " + checks + " failed");
    }
}
