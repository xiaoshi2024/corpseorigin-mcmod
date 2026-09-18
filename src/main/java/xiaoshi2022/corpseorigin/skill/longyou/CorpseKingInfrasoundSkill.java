package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·尸王次声波。
 * <p>
 * 一次释放展开 3 秒声场（{@link InfrasoundFieldHandler}），三件事同时发生：
 * <ul>
 *   <li>频率共振 → 震散飞行中的箭矢/弩矢/三叉戟（只解体投掷物，不损坏手持武器）；</li>
 *   <li>内力三档判定 → 内力足够免疫、不足则被抽内力并削弱、完全无内力则晕厥受伤；</li>
 *   <li>操控范围内的尸兄（{@code corpseorigin:infrasound_controlled}）跟随自己、扑向自己的目标。</li>
 * </ul>
 * 冷却 {@value #COOLDOWN} tick（12 秒）+ 消耗内力，属于"尸王虽强但并非无解"的策略性大招：
 * 时机不对、内力不够，就只能用来挡一发冷箭。
 */
public class CorpseKingInfrasoundSkill extends AbstractSkill {

    public static final String PATH = "corpse_king_infrasound";

    /** 冷却：12 秒 */
    private static final int COOLDOWN = 240;
    /** 内力消耗：龙右内力上限 200，一次放掉五分之一 */
    private static final int INNER_POWER_COST = 40;

    public CorpseKingInfrasoundSkill() {
        super(PATH, SkillType.ULTIMATE, COOLDOWN, INNER_POWER_COST);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        InfrasoundFieldHandler.start(player);
    }
}
