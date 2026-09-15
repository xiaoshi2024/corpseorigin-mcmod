package xiaoshi2022.corpseorigin.skill.fengmohuitailang;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 风魔灰太郎·红陨石之剑 —— 陨石铸剑，对尸王特攻。
 * <p>
 * 设定效果：挥动陨石所铸之剑释放红色剑气，对尸王/尸兄类目标造成额外伤害。
 * 冷却：30 秒（600 ticks）。
 * 特效：红陨石剑气。
 * <p>
 * TODO 实装：挥剑释放红色剑气 + 对尸王/尸兄类目标额外伤害。
 */
public class MeteorSwordSkill extends AbstractSkill {

    public static final String PATH = "meteor_sword";

    public MeteorSwordSkill() {
        super(PATH, SkillType.COMBAT, 600);
    }
}
