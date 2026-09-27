package xiaoshi2022.corpseorigin.skill.chapter;

import java.util.Arrays;

/** One bounded, selectable adaptation per donor; identifiers are save-format keys. */
public enum GourdTrait {
    SHEEP("sheep", Tier.PASSIVE, 6, 1200),
    COW("cow", Tier.PASSIVE, 6, 600),
    MOOSHROOM("mooshroom", Tier.PASSIVE, 6, 600),
    PIG("pig", Tier.PASSIVE, 8, 160),
    CHICKEN("chicken", Tier.PASSIVE, 4, 1200),
    CAT("cat", Tier.PASSIVE, 6, 2400),
    VILLAGER("villager", Tier.PASSIVE, 0, 20),
    DOLPHIN("dolphin", Tier.PASSIVE, 12, 1200),
    GOAT("goat", Tier.PASSIVE, 10, 180),
    ENDERMAN("enderman", Tier.NEUTRAL, 15, 240),
    WOLF("wolf", Tier.NEUTRAL, 12, 300),
    ZOMBIFIED_PIGLIN("zombified_piglin", Tier.NEUTRAL, 15, 300),
    LLAMA("llama", Tier.NEUTRAL, 5, 80),
    PANDA("panda", Tier.NEUTRAL, 10, 400),
    ZOMBIE("zombie", Tier.HOSTILE, 8, 120),
    SKELETON("skeleton", Tier.HOSTILE, 6, 80),
    CREEPER("creeper", Tier.HOSTILE, 25, 400),
    SPIDER("spider", Tier.HOSTILE, 8, 160),
    CAVE_SPIDER("cave_spider", Tier.HOSTILE, 10, 180),
    WITCH("witch", Tier.HOSTILE, 15, 240),
    SLIME("slime", Tier.HOSTILE, 12, 200),
    MAGMA_CUBE("magma_cube", Tier.HOSTILE, 15, 240),
    BLAZE("blaze", Tier.HOSTILE, 18, 240),
    GHAST("ghast", Tier.HOSTILE, 25, 320),
    GUARDIAN("guardian", Tier.HOSTILE, 15, 200),
    ELDER_GUARDIAN("elder_guardian", Tier.ELITE, 25, 400),
    SHULKER("shulker", Tier.HOSTILE, 18, 240),
    WITHER_SKELETON("wither_skeleton", Tier.HOSTILE, 15, 200),
    PHANTOM("phantom", Tier.HOSTILE, 18, 240),
    WARDEN("warden", Tier.ELITE, 35, 500),
    ENDER_DRAGON("ender_dragon", Tier.BOSS, 45, 600),
    WITHER("wither", Tier.BOSS, 40, 600);

    public enum Tier { PASSIVE, NEUTRAL, HOSTILE, ELITE, BOSS }
    public final String id;
    public final Tier tier;
    public final int blood, cooldown;
    GourdTrait(String id, Tier tier, int blood, int cooldown) {
        this.id = id; this.tier = tier; this.blood = blood; this.cooldown = cooldown;
    }
    public String nameKey() { return "gourd.corpseorigin.trait." + id; }
    public String descriptionKey() { return nameKey() + ".desc"; }
    public boolean flying() { return this == BLAZE || this == GHAST || this == PHANTOM || this == ENDER_DRAGON || this == WITHER; }
    public static GourdTrait byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst().orElse(null);
    }
    public static GourdTrait donor(String entityId) {
        if (entityId == null || !entityId.startsWith("minecraft:")) return null;
        String path = entityId.substring("minecraft:".length());
        return switch (path) {
            case "ocelot" -> CAT;
            case "trader_llama" -> LLAMA;
            case "wandering_trader" -> VILLAGER;
            default -> byId(path);
        };
    }
    public static boolean succeeds(double roll, double chance, boolean alreadyKnown) {
        return !alreadyKnown && Double.isFinite(roll) && roll >= 0 && roll < 1
                && Double.isFinite(chance) && roll < Math.clamp(chance, 0, 1);
    }
    public static boolean bossEdible(float health, float maximum) {
        return Float.isFinite(health) && Float.isFinite(maximum) && health > 0 && maximum > 0 && health < maximum * .2F;
    }
}
