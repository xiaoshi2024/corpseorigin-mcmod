package xiaoshi2022.corpseorigin.skill.bianselong_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 变色龙尸兄·偷袭（捏心）——专属剧情技，近身捏碎目标心脏造成致命伤害。
 * <p>
 * 主动技能，冷却 60 秒（1200 ticks）。
 * <p>
 * 特效：偷袭特写动画。
 * <p>
 * TODO 实装：背身/近身判定 → 高额伤害 + 致死表现 + 特写镜头。
 */
public class HeartGrabAmbushSkill extends AbstractSkill {

    public static final String PATH = "heart_grab_ambush";

    public HeartGrabAmbushSkill() {
        super(PATH, SkillType.COMBAT, 1200);
    }
}
