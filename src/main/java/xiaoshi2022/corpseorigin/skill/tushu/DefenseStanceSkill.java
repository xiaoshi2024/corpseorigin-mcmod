package xiaoshi2022.corpseorigin.skill.tushu;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 屠叔·防御姿态 —— 短时大幅减伤。
 * <p>
 * 设定效果：进入防御姿态，短时间内大幅降低所受伤害。
 * 冷却：15 秒（300 ticks）。
 * 特效：护体气罩。
 * <p>
 * TODO 实装：持续减伤 buff + 气罩粒子表现。
 */
public class DefenseStanceSkill extends AbstractSkill {

    public static final String PATH = "defense_stance";

    public DefenseStanceSkill() {
        super(PATH, SkillType.DEFENSE, 300);
    }
}
