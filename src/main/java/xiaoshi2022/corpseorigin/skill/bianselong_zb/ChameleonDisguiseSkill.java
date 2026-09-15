package xiaoshi2022.corpseorigin.skill.bianselong_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 变色龙尸兄·伪装——切换为小惠的外观模型潜入。
 * <p>
 * 主动技能，冷却 30 秒（600 ticks）。
 * <p>
 * 特效：外观模型切换。
 * <p>
 * TODO 实装：切换显示模型/皮肤 + 伪装期间的名称与碰撞表现。
 */
public class ChameleonDisguiseSkill extends AbstractSkill {

    public static final String PATH = "chameleon_disguise";

    public ChameleonDisguiseSkill() {
        super(PATH, SkillType.UTILITY, 600);
    }
}
