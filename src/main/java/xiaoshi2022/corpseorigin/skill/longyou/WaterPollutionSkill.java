package xiaoshi2022.corpseorigin.skill.longyou;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·尸水之源——走过的水源会被污染成尸水。
 * <p>
 * 被动技能，无冷却，不进技能轮盘。
 * <p>
 * 实装见 {@code LongYouEventHandler}：服务端每隔几 tick 扫一圈玩家脚边，
 * 把普通<b>水源方块</b>换成尸水源；之后尸水会像水一样自己往低处流，
 * 接触到的玩家/村民照常中毒、被感染（那部分逻辑在 {@code InfectedWaterFluid} 里）。
 */
public class WaterPollutionSkill extends AbstractSkill {

    public static final String PATH = "water_pollution";

    /** 扫描半径（格，水平方向） */
    public static final int RADIUS = 2;
    /** 脚下一圈的高度：往下 1 格到往上 1 格，游在水里时也能覆盖到身体周围 */
    public static final int VERTICAL_RADIUS = 1;
    /** 单次扫描最多污染几个源块（一头扎进水塘时别把整片水域一口气换掉） */
    public static final int MAX_PER_SCAN = 6;

    public WaterPollutionSkill() {
        super(PATH, SkillType.UTILITY);
    }
}
