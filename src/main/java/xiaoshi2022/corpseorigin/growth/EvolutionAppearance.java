package xiaoshi2022.corpseorigin.growth;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** Appearance is selected once on the server and travels in the body attachment. */
public final class EvolutionAppearance {
    public static final String[] COLORS = {"red", "orange", "yellow", "green", "cyan", "blue", "purple"};
    private EvolutionAppearance() {}
    public static boolean initialize(CompoundTag body, RandomSource random) {
        if (!body.getBooleanOr("wings", false)) return false;
        boolean changed = false;
        int variant = body.getIntOr("wing_variant", -1);
        int color = body.getIntOr("wing_color", -1);
        if (variant < 0 || variant > 1) { body.putInt("wing_variant", random.nextInt(2)); changed = true; }
        if (color < 0 || color >= COLORS.length) { body.putInt("wing_color", random.nextInt(COLORS.length)); changed = true; }
        return changed;
    }
    public static String model(CompoundTag body) {
        if (!body.getBooleanOr("wings", false)) return "evolution_tail";
        return (body.getIntOr("wing_variant", 0) == 1 ? "evolution_feather" : "evolution_bat")
                + (body.getBooleanOr("gills", false) ? "_tail" : "");
    }
    public static String color(CompoundTag body) {
        return COLORS[Math.clamp(body.getIntOr("wing_color", 0), 0, COLORS.length - 1)];
    }
}
