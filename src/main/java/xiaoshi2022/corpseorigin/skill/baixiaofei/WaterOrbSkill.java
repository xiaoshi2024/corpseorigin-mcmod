package xiaoshi2022.corpseorigin.skill.baixiaofei;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 白小飞·水异能（水球）
 * <p>
 * 设定效果：发射水球弹，命中造成击退与短暂减速。
 * 冷却：8 秒。特效：蓝色水滴粒子，命中炸开水花。
 * <p>
 * TODO 实装：抛出/直线发射水球 → 命中判定 → 击退 + 减速 + 水花粒子。
 */
public class WaterOrbSkill extends AbstractSkill {

    public static final String PATH = "water_orb";

    public WaterOrbSkill() {
        super(PATH, SkillType.COMBAT, 160);   // 8s
    }
}
