package xiaoshi2022.corpseorigin.skill.longyou;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·雷电
 * <p>
 * 设定效果：近战抓取麻痹，或召唤落雷。
 * 冷却：10 秒。特效：雷电粒子 + 击飞。
 * <p>
 * TODO 实装：前方近战抓取麻痹，或向视线落点召唤落雷 → 范围伤害 + 击飞 + 雷电粒子。
 */
public class CorpseKingThunderSkill extends AbstractSkill {

    public static final String PATH = "corpse_king_thunder";

    public CorpseKingThunderSkill() {
        super(PATH, SkillType.COMBAT, 200);   // 10s
    }
}
