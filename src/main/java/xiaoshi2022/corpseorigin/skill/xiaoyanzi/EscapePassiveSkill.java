package xiaoshi2022.corpseorigin.skill.xiaoyanzi;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 小言子·跑路被动——附近有队友时自身速度 +1。
 * <p>
 * 被动技能，无冷却，不进技能轮盘。
 * <p>
 * 特效：无。
 * <p>
 * TODO 实装：每 tick 检测附近队友数量 → 给予/移除速度增益。
 */
public class EscapePassiveSkill extends AbstractSkill {

    public static final String PATH = "escape_passive";

    public EscapePassiveSkill() {
        super(PATH, SkillType.UTILITY);
    }
}
