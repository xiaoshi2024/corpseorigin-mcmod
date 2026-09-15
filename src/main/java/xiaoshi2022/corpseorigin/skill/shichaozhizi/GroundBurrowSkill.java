package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 尸巢之子·地遁突袭——潜入地下后从玩家脚下破土冲出，造成高伤害。
 * <p>
 * 主动技能，冷却 20 秒（400 ticks）。
 * <p>
 * 特效：破土粒子 + 地面裂纹。
 * <p>
 * TODO 实装：潜地状态 → 目标脚下破土 → 范围伤害 + 击退。
 */
public class GroundBurrowSkill extends AbstractSkill {

    public static final String PATH = "ground_burrow";

    public GroundBurrowSkill() {
        super(PATH, SkillType.COMBAT, 400);
    }
}
