package xiaoshi2022.corpseorigin.skill.k;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * K·血翼黑刃 —— 近战血刃攻击并吸取生命。
 * <p>
 * 设定效果：挥出血翼黑刃重击近身目标，并将造成的伤害转化为自身生命回复。
 * 冷却：10 秒（200 ticks）。
 * 特效：血刃武器模型 + 血粒子。
 * <p>
 * TODO 实装：近战重击 + 吸血回复 + 血刃武器模型。
 */
public class BloodWingBladeSkill extends AbstractSkill {

    public static final String PATH = "blood_wing_blade";

    public BloodWingBladeSkill() {
        super(PATH, SkillType.COMBAT, 200);
    }
}
