package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 天线宝宝尸兄·吸食——近战抓住玩家持续吸血，可用打断技解救。
 * <p>
 * 主动技能，无冷却（0 ticks）。
 * <p>
 * 特效：吸取粒子 + 被抓动画。
 * <p>
 * TODO 实装：抓取判定 → 持续吸血 → 受击/打断技解救。
 */
public class LifeDrainSuckSkill extends AbstractSkill {

    public static final String PATH = "life_drain_suck";

    public LifeDrainSuckSkill() {
        super(PATH, SkillType.COMBAT, 0);
    }
}
