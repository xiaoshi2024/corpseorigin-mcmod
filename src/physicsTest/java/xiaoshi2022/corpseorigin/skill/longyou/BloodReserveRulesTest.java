package xiaoshi2022.corpseorigin.skill.longyou;

public final class BloodReserveRulesTest {
    private static int checks;
    public static void main(String[] args) {
        check(!BloodReserveRules.eligible("jingang_zb", true, 8), "Below threshold");
        check(BloodReserveRules.eligible("corpse_brother", true, 1), "Free corpse starts with flesh reserve");
        check(BloodReserveRules.eligible("corpse_brother", true, 9), "Unlock at heaven tier");
        check(BloodReserveRules.eligible("jingang_zb", true, 20), "High-level corpse");
        check(!BloodReserveRules.eligible("mortal", false, 20), "Human excluded at maximum level");
        check(!BloodReserveRules.eligible("corpse_brother", false, 9), "Lost corpse body");
        for (String role : new String[]{"longyou", "zuohufa", "shichaozhizi", "kaiweinai"}) {
            check(BloodReserveRules.eligible(role, true, 1), role + " innate reserve");
            check(BloodReserveRules.eligible(role, false, 1), role + " before corpse state synchronization");
        }
        check(!BloodReserveRules.eligible(null, false, 9), "Missing role");
        check(BloodReserveRules.eligible(null, true, 9), "Unnamed evolved corpse");
        System.out.println("Blood reserve eligibility: " + checks + " regression checks passed.");
    }
    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
}
