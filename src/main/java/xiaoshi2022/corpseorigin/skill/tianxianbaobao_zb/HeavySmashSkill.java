package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 天线宝宝尸兄·巨力拍击——高伤害近战拍击，附带击退。
 * <p>
 * 主动技能，冷却 6 秒（120 ticks）。
 * <p>
 * 特效：拍击震地粒子。
 * <p>
 * TODO 实装：前方范围重击 + 击退 + 震地粒子。
 */
public class HeavySmashSkill extends AbstractSkill {

    public static final String PATH = "heavy_smash";

    public HeavySmashSkill() {
        super(PATH, SkillType.COMBAT, 120);
    }
}
