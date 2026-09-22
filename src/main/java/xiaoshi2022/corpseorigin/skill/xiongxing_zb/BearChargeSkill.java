package xiaoshi2022.corpseorigin.skill.xiongxing_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 熊型尸兄·巨力冲撞 —— 向前冲撞，撞飞路径上的敌人（熊大熊二共用同一套动作特效）。
 * <p>
 * 冷却：10 秒（200 ticks）。
 * <p>
 * 特效：冲撞拖影 + 震地粒子。
 * <p>
 * TODO 实装：冲刺位移 + 路径碰撞伤害与击退。
 */
public class BearChargeSkill extends AbstractSkill {
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.rush(p,false);
    }

    public static final String PATH = "bear_charge";

    public BearChargeSkill() {
        super(PATH, SkillType.COMBAT, 200);
    }
}
