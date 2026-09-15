package xiaoshi2022.corpseorigin.skill.zuohufa;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 左护法·南面拦截 —— 封锁区域并召唤小怪拦路。
 * <p>
 * 设定效果：封锁南面区域阻挡通行，并召唤若干小怪拦截闯入者。
 * 冷却：30 秒（600 ticks）。
 * 特效：区域封锁结界 + 召唤光柱。
 * <p>
 * TODO 实装：范围封锁区域（进不去/出不来或减速）+ 召唤若干小怪。
 */
public class SouthBlockadeSkill extends AbstractSkill {

    public static final String PATH = "south_blockade";

    public SouthBlockadeSkill() {
        super(PATH, SkillType.UTILITY, 600);
    }
}
