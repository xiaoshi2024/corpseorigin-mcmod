package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 天线宝宝尸兄·天线格挡——被动：受到的斧头、箭矢/投掷物伤害降低。
 * <p>
 * 被动技能，无冷却，不进技能轮盘。
 * <p>
 * 判定与实装在 {@code xiaoshi2022.corpseorigin.event.TianXianBaoBaoEventHandler}：
 * 那里会挑出"斧头"（{@code minecraft:axes} 标签）和"箭矢/投掷物"（{@code AbstractArrow}，
 * 含三叉戟）两类伤害，按 {@link #DAMAGE_REDUCTION} 减免。
 */
public class AntennaBlockSkill extends AbstractSkill {

    public static final String PATH = "antenna_block";

    /** 这两类伤害的减免比例：30% */
    public static final float DAMAGE_REDUCTION = 0.30F;

    public AntennaBlockSkill() {
        super(PATH, SkillType.DEFENSE);
    }
}
