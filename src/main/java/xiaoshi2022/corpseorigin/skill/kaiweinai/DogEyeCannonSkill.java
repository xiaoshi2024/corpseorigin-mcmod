package xiaoshi2022.corpseorigin.skill.kaiweinai;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 开胃奶·狗眼炮——发射哈姆造成混乱/爆炸伤害。
 * <p>
 * 主动技能，冷却 12 秒（240 ticks）。
 * <p>
 * 特效：盒子模型 + 哈姆发射。
 * <p>
 * TODO 实装：发射投掷物 → 命中爆炸 + 混乱效果 + 哈姆模型。
 */
public class DogEyeCannonSkill extends AbstractSkill {

    public static final String PATH = "dog_eye_cannon";

    public DogEyeCannonSkill() {
        super(PATH, SkillType.COMBAT, 240);
    }
}
