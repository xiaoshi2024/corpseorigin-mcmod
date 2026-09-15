package xiaoshi2022.corpseorigin.skill.bianyi_guiyu;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 变异鲑鱼·水战撕咬 —— 水下突袭撕咬。
 * <p>
 * 冷却：6 秒（120 ticks）。
 * <p>
 * 特效：水花粒子 + 突袭动画。
 * <p>
 * TODO 实装：水下突进 + 撕咬伤害 + 水花粒子。
 */
public class WaterBiteSkill extends AbstractSkill {

    public static final String PATH = "water_bite";

    public WaterBiteSkill() {
        super(PATH, SkillType.COMBAT, 120);
    }
}
