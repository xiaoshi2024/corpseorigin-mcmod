package xiaoshi2022.corpseorigin.skill.longyou;

public final class RawMeatDigestionRulesTest {
    private static int checks;

    public static void main(String[] args) {
        check(RawMeatDigestionRules.accepted(0, 600, 0) == 5, "One meal gives five points");
        check(RawMeatDigestionRules.accepted(0, 600, 19) == 1, "Partial queue capacity");
        check(RawMeatDigestionRules.accepted(0, 600, 20) == 0, "Full queue");
        check(RawMeatDigestionRules.accepted(598, 600, 0) == 2, "Near-full blood");
        check(RawMeatDigestionRules.accepted(598, 600, 2) == 0, "Pending blood already covers deficit");
        check(RawMeatDigestionRules.accepted(600, 600, 0) == 0, "Full blood");

        var step = new RawMeatDigestionRules.Step(5, 0, 0);
        int gained = 0;
        for (int tick = 1; tick <= 200; tick++) {
            step = RawMeatDigestionRules.tick(step.pending(), step.ticks(), true, false);
            gained += step.gained();
            check(gained == tick / 40, "Exactly one point every 40 ticks: " + tick);
        }
        check(step.pending() == 0 && gained == 5, "Meal exhausted after ten seconds");
        check(RawMeatDigestionRules.tick(0, 0, true, false).gained() == 0, "No free regeneration");

        // A second meal must neither accelerate nor reset the current digestion interval.
        step = RawMeatDigestionRules.tick(5, 19, true, false);
        int extra = RawMeatDigestionRules.accepted(100, 600, step.pending());
        step = RawMeatDigestionRules.tick(step.pending() + extra, step.ticks(), true, false);
        check(step.pending() == 10 && step.ticks() == 21 && step.gained() == 0, "Meals share one timer");
        step = RawMeatDigestionRules.tick(20, 39, true, false);
        check(step.pending() == 19 && step.gained() == 1, "Full queue does not multiply rate");
        check(RawMeatDigestionRules.tick(20, 39, false, false).equals(
                new RawMeatDigestionRules.Step(0, 0, 0)), "Death, spectator or lost eligibility clears digestion");
        check(RawMeatDigestionRules.tick(20, 39, true, true).equals(
                new RawMeatDigestionRules.Step(0, 0, 0)), "Full blood clears pending without overflow");
        System.out.println("Raw meat digestion: " + checks + " checks passed.");
    }

    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
}
