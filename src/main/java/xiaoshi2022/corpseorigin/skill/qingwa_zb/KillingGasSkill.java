package xiaoshi2022.corpseorigin.skill.qingwa_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 青蛙尸兄·杀势刺激气体——空气中吞吐刺激气体，范围增幅杀势、触发狂暴。
 * <p>
 * 主动技能，冷却 15 秒（300 ticks）。
 * <p>
 * 特效：毒气粒子 + 杀势增幅提示。
 * <p>
 * TODO 实装：范围气体区域 → 范围内目标获得杀势/狂暴增益 + 毒气粒子。
 */
public class KillingGasSkill extends AbstractSkill {

    public static final String PATH = "killing_gas";

    public KillingGasSkill() {
        super(PATH, SkillType.UTILITY, 300);
    }
}
