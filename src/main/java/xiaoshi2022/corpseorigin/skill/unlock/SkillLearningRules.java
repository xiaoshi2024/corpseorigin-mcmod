package xiaoshi2022.corpseorigin.skill.unlock;

import java.util.Set;
import xiaoshi2022.corpseorigin.skill.SkillType;

/** Explicit exceptions before conservative default weights; no migration removes learned skills. */
public final class SkillLearningRules {
    public static final Set<String> THUNDER = Set.of("thunder_power", "corpse_king_thunder", "natural_judgment");

    /**
     * 跨角色「获取式」学习时<b>排除</b>的形态 / 身体改造类技能。
     * <p>
     * 这些技能会换身体、改身份或叠形态，跨角色挂在别人身上会和尸兄状态、形态系统互斥
     * （现在没有任何防护）。所以固定角色只能学到别人的<b>招式</b>，学不到别人的<b>身体</b>。
     * <p>
     * 注意：这里列的大多本来就没有获取式来源，列出来是防止以后有人给它们加了来源就顺手跨过去了。
     */
    public static final Set<String> CROSS_ROLE_EXCLUDED = Set.of(
            "black_gold_heart",           // 植入后转化为尸兄形态
            "body_possession",            // 附身换身
            "flesh_reshape", "flesh_abandon",  // 血肉重塑 / 弃壳
            "xuanwu_body", "peel_shell", "jingang_infant_convergence",  // 借体 / 脱壳形态
            "transform");                 // 变身

    private SkillLearningRules() {}

    /**
     * 固定角色能否跨角色学到这个技能。
     * <p>
     * 判据是「<b>获取式</b>」：技能声明了解锁来源（拿到某件东西 / 遇到指定事物就学会）。
     * 纯技能树技能不在此列 —— 那些仍然只属于原角色，必须用原角色花点学习。
     * 形态 / 身体改造类一并排除，见 {@link #CROSS_ROLE_EXCLUDED}。
     */
    public static boolean crossLearnable(xiaoshi2022.corpseorigin.skill.ISkill skill) {
        return !skill.getUnlockSources().isEmpty()
                && !CROSS_ROLE_EXCLUDED.contains(skill.getId().getPath());
    }

    public static boolean innate(String role, String path) {
        return "longyou".equals(role) && !THUNDER.contains(path)
                || "xiaojingang".equals(role) && path.startsWith("gourd_")
                || ("guigun_human".equals(role) || "guigun_corpse".equals(role)) && path.startsWith("guigun_")
                || "hei_wuchou".equals(role) && path.startsWith("wuchou_")
                || "bai_wusheng".equals(role) && path.startsWith("wusheng_");
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
