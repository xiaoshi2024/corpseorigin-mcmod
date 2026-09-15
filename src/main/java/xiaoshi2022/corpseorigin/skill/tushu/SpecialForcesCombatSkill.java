package xiaoshi2022.corpseorigin.skill.tushu;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 屠叔·特能队体术 —— 近战基础连击。
 * <p>
 * 设定效果：炎黄特能队主力体术，对近身目标打出快速连击。
 * 冷却：3 秒（60 ticks）。
 * 特效：体术打击粒子。
 * <p>
 * TODO 实装：连续三段近战判定 + 击退。
 */
public class SpecialForcesCombatSkill extends AbstractSkill {

    public static final String PATH = "special_forces_combat";

    public SpecialForcesCombatSkill() {
        super(PATH, SkillType.COMBAT, 60);
    }
}
