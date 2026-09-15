package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 尸巢之子·血莲护身甲（阶段变身）——血条过半自动进入二阶段，攻防提升。
 * <p>
 * 被动技能，无冷却，不进技能轮盘。
 * <p>
 * 特效：血莲护甲模型。
 * <p>
 * TODO 实装：血量阈值检测 → 二阶段属性强化 + 护甲模型/特效。
 */
public class BloodLotusArmorSkill extends AbstractSkill {

    public static final String PATH = "blood_lotus_armor";

    public BloodLotusArmorSkill() {
        super(PATH, SkillType.DEFENSE);
    }
}
