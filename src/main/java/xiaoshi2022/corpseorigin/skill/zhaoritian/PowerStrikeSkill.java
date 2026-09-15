package xiaoshi2022.corpseorigin.skill.zhaoritian;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 赵日天·强力一击 —— 专属秒杀剧情技。
 * <p>
 * 冷却：20 秒（400 ticks）。
 * <p>
 * 特效：登场特写动画 + 秒杀打击特效。
 * <p>
 * TODO 实装：单体重击，对低血量/杂兵目标触发秒杀表现 + 打击特效。
 */
public class PowerStrikeSkill extends AbstractSkill {

    public static final String PATH = "power_strike";

    public PowerStrikeSkill() {
        super(PATH, SkillType.COMBAT, 400);
    }
}
