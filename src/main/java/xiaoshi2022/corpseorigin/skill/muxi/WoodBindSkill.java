package xiaoshi2022.corpseorigin.skill.muxi;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 木犀·木系束缚 —— 以藤蔓定身目标。
 * <p>
 * 设定效果：催生木系藤蔓束缚前方目标，限制其行动并持续造成伤害。
 * 冷却：10 秒（200 ticks）。
 * 特效：藤蔓束缚粒子。
 * <p>
 * TODO 实装：视线/前方目标定身 + 持续伤害 + 藤蔓模型。
 */
public class WoodBindSkill extends AbstractSkill {

    public static final String PATH = "wood_bind";

    public WoodBindSkill() {
        super(PATH, SkillType.COMBAT, 200);
    }
}
