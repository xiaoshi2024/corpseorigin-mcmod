package xiaoshi2022.corpseorigin.skill.unlock;

import java.util.Set;
import xiaoshi2022.corpseorigin.skill.SkillType;

/** Explicit exceptions before conservative default weights; no migration removes learned skills. */
public final class SkillLearningRules {
    public static final Set<String> THUNDER = Set.of("thunder_power", "corpse_king_thunder", "natural_judgment");
    private SkillLearningRules() {}
    public static boolean innate(String role, String path) {
        return "longyou".equals(role) && !THUNDER.contains(path)
                || "xiaojingang".equals(role) && path.startsWith("gourd_");
    }
    public static int cost(String path, SkillType type, int cooldown, boolean active) {
        return switch (path) {
            case "thunder_power" -> 8;
            case "corpse_king_thunder" -> 15;
            case "natural_judgment" -> 30;
            case "spatial_blink" -> 8;
            default -> type == SkillType.ULTIMATE ? 15 : !active ? 4
                    : type == SkillType.UTILITY ? 3 : cooldown >= 600 ? 8 : cooldown >= 200 ? 5 : 3;
        };
    }
    public static int level(String path, SkillType type) {
        return switch (path) {
            case "thunder_power" -> 2;
            case "corpse_king_thunder" -> 3;
            case "natural_judgment" -> 5;
            default -> type == SkillType.ULTIMATE ? 3 : 1;
        };
    }
}
