package xiaoshi2022.corpseorigin.skill.unlock;

import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.Set;

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

    /**
     * 鬼棍两种形态天生的招式 —— <b>按形态给，不按前缀混着给</b>。
     * <p>
     * 人类形态只有三节棍棍法与金针刺穴，尸兄形态只有尸棍那两招。
     * 早先按 {@code guigun_} 前缀一刀切，会让人类鬼棍把尸棍招式也记进"已学会"
     * （虽然放不出来，但那是别人的招式，不该算在他头上）。
     */
    private static final Set<String> GUIGUN_HUMAN_SKILLS = Set.of("guigun_sweep", "guigun_guard");
    private static final Set<String> GUIGUN_CORPSE_SKILLS = Set.of("guigun_resonance", "guigun_crush");

    public static boolean innate(String role, String path) {
        return "longyou".equals(role) && !THUNDER.contains(path)
                || "xiaojingang".equals(role) && path.startsWith("gourd_")
                || "guigun_human".equals(role) && GUIGUN_HUMAN_SKILLS.contains(path)
                || "guigun_corpse".equals(role) && GUIGUN_CORPSE_SKILLS.contains(path)
                || "hei_wuchou".equals(role) && path.startsWith("wuchou_")
                || "bai_wusheng".equals(role) && path.startsWith("wusheng_");
    }
    /**
     * 学习消耗的进化点（技能树用）。
     * <p>
     * 档位（保守梯度，点数整体上调一档）：
     * <pre>
     * 普通招式（冷却 &lt; 200）      4
     * 中坚招式（冷却 200 ~ 599）   6
     * 强力招式（冷却 ≥ 600）       10
     * 辅助（UTILITY）              4
     * 被动                         5
     * 终极（ULTIMATE）             18
     * </pre>
     * 下面几个 {@code case} 是已经单独调过的例外（龙右雷系 / 空间异能），保持原值。
     */
    public static int cost(String path, SkillType type, int cooldown, boolean active) {
        return switch (path) {
            case "thunder_power" -> 8;
            case "corpse_king_thunder" -> 15;
            case "natural_judgment" -> 30;
            case "spatial_blink" -> 8;
            default -> type == SkillType.ULTIMATE ? 18 : !active ? 5
                    : type == SkillType.UTILITY ? 4 : cooldown >= 600 ? 10 : cooldown >= 200 ? 6 : 4;
        };
    }

    /**
     * 学习所需的最低进化等级（技能树用）。等级 → 阶层的换算见 {@code EvolutionTier}：
     * 人 1-4 / 地 5-8 / 天 9 / 神 10-12。
     * <p>
     * 档位（保守梯度，整体只抬一格）：
     * <pre>
     * 辅助（UTILITY）/ 被动 / 普通招式（冷却 &lt; 200）   2（人2）
     * 中坚招式（冷却 200 ~ 599）                       3（人3）
     * 强力招式（冷却 ≥ 600）                           4（人4）
     * 终极（ULTIMATE）                                 5（地1）
     * </pre>
     * 辅助 / 被动优先于冷却判定（与 {@link #cost} 的优先级一致）。
     * 下面几个 {@code case} 是已经单独调过的例外，保持原值。
     */
    public static int level(String path, SkillType type, int cooldown, boolean active) {
        return switch (path) {
            case "thunder_power" -> 2;
            case "corpse_king_thunder" -> 3;
            case "natural_judgment" -> 5;
            default -> type == SkillType.ULTIMATE ? 5 : !active ? 2
                    : type == SkillType.UTILITY ? 2 : cooldown >= 600 ? 4 : cooldown >= 200 ? 3 : 2;
        };
    }
}
