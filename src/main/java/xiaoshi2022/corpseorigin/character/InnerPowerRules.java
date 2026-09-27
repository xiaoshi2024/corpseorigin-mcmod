package xiaoshi2022.corpseorigin.character;

import xiaoshi2022.corpseorigin.skill.EvolutionManager;

/** Shared capacity rules for innate, learned and Tian-tier qi. */
public final class InnerPowerRules {
    public static final int AWAKENING_LEVEL = 9;

    private InnerPowerRules() {}

    public static int growthCapacity(int level) {
        return 100 + 20 * (Math.clamp(level, 1, EvolutionManager.MAX_LEVEL) - 1);
    }

    public static boolean awakensAtTier(int innate, int learned, int level) {
        return innate <= 0 && learned <= 0 && level >= AWAKENING_LEVEL;
    }

    public static int capacity(int innate, int learned, int level) {
        if (awakensAtTier(innate, learned, level)) return growthCapacity(level);
        return Math.max(0, Math.max(innate, learned));
    }
}
