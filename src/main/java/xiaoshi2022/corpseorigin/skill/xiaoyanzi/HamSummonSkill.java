package xiaoshi2022.corpseorigin.skill.xiaoyanzi;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 小言子·哈姆召唤——召唤哈姆喷吐火球。
 * <p>
 * 主动技能，冷却 10 秒（200 ticks）。
 * <p>
 * 特效：小火焰粒子。
 * <p>
 * TODO 实装：召唤喷火蜥蜴实体/临时伙伴 + 火球攻击。
 */
public class HamSummonSkill extends AbstractSkill {

    public static final String PATH = "ham_summon";

    public HamSummonSkill() {
        super(PATH, SkillType.COMBAT, 200);
    }
}
