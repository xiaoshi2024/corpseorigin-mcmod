package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 龙右·自然审判！真·球状闪电。
 * <p>
 * 设定上这是尸王威力最大的一手：原体离体时引发的球状闪电，足以毁灭整个京城。
 * <p>
 * 实装成一枚飞出去的球状闪电：一路放电、把周围的敌人往球心拽，
 * 撞到东西或到期后炸开 —— 一圈紫色电弧 + 大范围高伤 + 把地形抹平
 * （见 {@link ThunderStrikeHandler#ballLightning}）。
 * <p>
 * 终极技能：60 秒冷却 + 80 点内力，前置 {@code thunder_power}（雷电之力）。
 */
public class BallLightningSkill extends AbstractSkill {

    public static final String PATH = "natural_judgment";

    /** 冷却：60 秒 */
    private static final int COOLDOWN = 1200;
    /** 内力消耗：龙右内力上限 200，一发吃掉四成 */
    private static final int INNER_POWER_COST = 80;

    public BallLightningSkill() {
        super(PATH, SkillType.ULTIMATE, COOLDOWN, INNER_POWER_COST);
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(CorpseOrigin.id(ThunderPowerSkill.PATH));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ThunderStrikeHandler.ballLightning(player);
    }
}
