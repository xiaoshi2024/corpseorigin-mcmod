package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 龙右·雷鳗。
 * <p>
 * 能力来源：吸收「电鳗」尸兄之后掌握的第一手雷电招式。
 * <p>
 * 向视线上的敌人（没瞄到人就打视线落点）召唤一道巨大的紫色天雷，威力足以击碎大楼 ——
 * 落点会造成范围伤害、击飞、麻痹，并把地形轰出一个坑（见 {@link ThunderStrikeHandler#eelStrike}）。
 * <p>
 * 前置：{@code thunder_power}（雷电之力）。
 */
public class CorpseKingThunderSkill extends AbstractSkill {

    public static final String PATH = "corpse_king_thunder";

    /** 冷却：10 秒 */
    private static final int COOLDOWN = 200;
    /** 内力消耗 */
    private static final int INNER_POWER_COST = 25;

    public CorpseKingThunderSkill() {
        super(PATH, SkillType.COMBAT, COOLDOWN, INNER_POWER_COST);
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(CorpseOrigin.id(ThunderPowerSkill.PATH));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ThunderStrikeHandler.eelStrike(player);
    }
}
