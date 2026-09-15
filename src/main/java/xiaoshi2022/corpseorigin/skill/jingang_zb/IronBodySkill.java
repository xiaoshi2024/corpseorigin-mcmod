package xiaoshi2022.corpseorigin.skill.jingang_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 金刚尸兄·刀枪不入（二阶）——正面免疫普通攻击，需要攻击头部弱点。
 * <p>
 * 被动技能，无冷却，不进技能轮盘。
 * <p>
 * 特效：金属光泽皮肤。
 * <p>
 * TODO 实装：正面伤害免疫判定 + 头部弱点伤害加成 + 皮肤光效。
 */
public class IronBodySkill extends AbstractSkill {

    public static final String PATH = "iron_body";

    public IronBodySkill() {
        super(PATH, SkillType.DEFENSE);
    }
}
