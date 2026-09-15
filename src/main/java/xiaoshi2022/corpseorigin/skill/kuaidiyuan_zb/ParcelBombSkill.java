package xiaoshi2022.corpseorigin.skill.kuaidiyuan_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 快递员尸兄·包裹爆炸 —— 投掷包裹造成简易爆炸伤害。
 * <p>
 * 冷却：10 秒（200 ticks）。
 * <p>
 * 特效：包裹道具模型 + 简易爆炸特效。
 * <p>
 * TODO 实装：投掷包裹实体（或抛物线判定）+ 落点爆炸伤害。
 */
public class ParcelBombSkill extends AbstractSkill {

    public static final String PATH = "parcel_bomb";

    public ParcelBombSkill() {
        super(PATH, SkillType.COMBAT, 200);
    }
}
