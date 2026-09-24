package xiaoshi2022.corpseorigin.growth;

/** Mounting appearance does not grant the associated survival ability. */
public final class OrganAccessRules {
    private OrganAccessRules() {}
    public static boolean mayMount(boolean creative, boolean corpse, int level, String trait, boolean unlocked) {
        return creative || corpse && unlocked;
    }
}
