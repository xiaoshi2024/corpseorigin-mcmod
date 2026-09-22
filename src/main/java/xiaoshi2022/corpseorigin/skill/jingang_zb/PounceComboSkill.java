package xiaoshi2022.corpseorigin.skill.jingang_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 金刚尸兄·扑咬连击——三连扑击，命中附带流血。
 * <p>
 * 主动技能，冷却 8 秒（160 ticks）。
 * <p>
 * 特效：扑击拖影。
 * <p>
 * TODO 实装：三段位移扑击 + 命中流血 + 拖影粒子。
 */
public class PounceComboSkill extends AbstractSkill {
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.rush(p,true);
    }

    public static final String PATH = "pounce_combo";

    public PounceComboSkill() {
        super(PATH, SkillType.COMBAT, 160);
    }
}
