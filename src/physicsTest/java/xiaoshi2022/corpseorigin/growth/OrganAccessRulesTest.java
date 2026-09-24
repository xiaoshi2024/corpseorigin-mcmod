package xiaoshi2022.corpseorigin.growth;

public final class OrganAccessRulesTest {
    public static void main(String[] args) {
        check(OrganAccessRules.mayMount(true, false, 1, "wings", false), "Creative human can mount");
        check(OrganAccessRules.mayMount(true, true, 1, "gills", false), "Creative corpse can mount");
        check(!OrganAccessRules.mayMount(false, true, 9, "wings", false), "Level nine still needs unlock");
        check(!OrganAccessRules.mayMount(false, true, 20, "vampire", false), "High level still needs unlock");
        check(!OrganAccessRules.mayMount(false, true, 8, "wings", false), "Low level needs evolution");
        check(OrganAccessRules.mayMount(false, true, 1, "gills", true), "Evolved low level can mount");
        check(!OrganAccessRules.mayMount(false, true, 1, "cosmetic", false), "Cosmetics need their own unlock");
        check(!OrganAccessRules.mayMount(false, false, 20, "wings", true), "Survival human requires corpse body");
        System.out.println("Organ access: 8 regression checks passed.");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
