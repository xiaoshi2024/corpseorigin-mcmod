package xiaoshi2022.corpseorigin.skill.chongmu;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 虫母·召唤虫群
 * <p>
 * 设定效果：召唤红火蚁与巨大子弹蚁虫群（两种虫类共用虫母召唤技能，仅区分外观模型）。
 * 冷却：30 秒。特效：虫群生成特效。
 * <p>
 * TODO 实装：召唤若干虫群实体 → 群体近战 + 毒素/爆炸伤害 + 生成特效。
 */
public class SummonSwarmSkill extends AbstractSkill {

    public static final String PATH = "summon_swarm";

    public SummonSwarmSkill() {
        super(PATH, SkillType.COMBAT, 600);   // 30s
    }
}
