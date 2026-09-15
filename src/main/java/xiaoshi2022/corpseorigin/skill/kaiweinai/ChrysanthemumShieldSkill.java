package xiaoshi2022.corpseorigin.skill.kaiweinai;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 开胃奶·菊花盾·开——展开正面无敌盾，持续期间不能攻击。
 * <p>
 * 主动技能，冷却 20 秒（400 ticks），持续 5 秒。
 * <p>
 * 特效：巨大花状护盾模型。
 * <p>
 * TODO 实装：正面无敌判定 → 禁攻击 → 护盾模型与粒子。
 */
public class ChrysanthemumShieldSkill extends AbstractSkill {

    public static final String PATH = "chrysanthemum_shield";

    public ChrysanthemumShieldSkill() {
        super(PATH, SkillType.DEFENSE, 400);
    }
}
