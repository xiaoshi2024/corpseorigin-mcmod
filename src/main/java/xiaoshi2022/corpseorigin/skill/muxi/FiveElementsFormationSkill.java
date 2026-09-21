package xiaoshi2022.corpseorigin.skill.muxi;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 木犀·五行阵 —— 展开团队增益 / 区域控制阵法。
 * <p>
 * 设定效果：作为五行阵核心展开大阵，为队友提供增益并压制阵中敌人。
 * 冷却：30 秒（600 ticks）。
 * 特效：五行阵图纸与光柱。
 * <p>
 * TODO 实装：阵法区域 → 队友增益 + 敌人减速/压制。
 */
public class FiveElementsFormationSkill extends AbstractSkill {

    public static final String PATH = "five_elements_formation";

    public FiveElementsFormationSkill() {
        super(PATH, SkillType.ULTIMATE, 600, 15);
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer player) {
        xiaoshi2022.corpseorigin.skill.chapter.FiveElementsCombat.formation(player);
    }
}
