package xiaoshi2022.corpseorigin.skill.chongqun;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 虫群·群体撕咬 —— 群体近战撕咬，附带毒素/爆炸伤害（红火蚁与巨大子弹蚁共用，仅区分外观模型）。
 * <p>
 * 冷却：2 秒（40 ticks）。
 * <p>
 * 特效：虫群撕咬粒子。
 * <p>
 * TODO 实装：近身群体判定 + 中毒/爆炸效果 + 两种虫群外观区分。
 */
public class SwarmBiteSkill extends AbstractSkill {
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.swarmBite(p);
    }

    public static final String PATH = "swarm_bite";

    public SwarmBiteSkill() {
        super(PATH, SkillType.COMBAT, 40);
    }
}
