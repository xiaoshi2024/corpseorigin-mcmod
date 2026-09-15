package xiaoshi2022.corpseorigin.skill.yanyan;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 炎燕·五行逆阵·火球 —— 大范围压制伤害的巨型火球。
 * <p>
 * 设定效果：作为五行逆阵核心蓄力放出巨型火球，对落点周边造成大范围压制伤害。
 * 冷却：30 秒（600 ticks）。
 * 特效：逆阵粒子特效 + 巨型火球释放动画。
 * <p>
 * TODO 实装：蓄力 → 巨型火球投射物 → 落点大范围灼烧伤害。
 */
public class ReverseFormationFireballSkill extends AbstractSkill {

    public static final String PATH = "reverse_formation_fireball";

    public ReverseFormationFireballSkill() {
        super(PATH, SkillType.ULTIMATE, 600);
    }
}
