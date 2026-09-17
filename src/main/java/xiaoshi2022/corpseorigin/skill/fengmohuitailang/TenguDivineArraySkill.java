package xiaoshi2022.corpseorigin.skill.fengmohuitailang;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 风魔灰太郎·忍术·大天狗神御 —— 发动究极秘阵，对不死系目标造成压制与克制伤害。
 * <p>
 * 设定效果：展开大天狗神御秘阵，对进入阵中的目标持续压制，并对不死髅体类目标额外克制。
 * 冷却：90 秒（1800 ticks）。
 * 特效：忍者阵法特效 + 大天狗号飞船。
 * <p>
 * TODO 实装：阵法展开 → 区域持续伤害 + 对不死髅体类目标额外克制。
 */
public class TenguDivineArraySkill extends AbstractSkill {

    public static final String PATH = "tengu_divine_array";

    public TenguDivineArraySkill() {
        super(PATH, SkillType.ULTIMATE, 1800);
    }
}
