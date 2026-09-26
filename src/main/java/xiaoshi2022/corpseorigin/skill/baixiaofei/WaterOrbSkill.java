package xiaoshi2022.corpseorigin.skill.baixiaofei;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 白小飞·水异能（水球）
 * <p>
 * 设定效果：发射水球弹，命中造成击退与短暂减速。
 * 冷却：8 秒。特效：蓝色水滴粒子，命中炸开水花。
 * <p>
 * TODO 实装：抛出/直线发射水球 → 命中判定 → 击退 + 减速 + 水花粒子。
 */
public class WaterOrbSkill extends AbstractSkill {

    public static final String PATH = "water_orb";

    public WaterOrbSkill() {
        super(PATH, SkillType.COMBAT, 160);   // 8s
    }

    /**
     * 水异能是白小飞在《尸巢之战篇》后期才觉醒的异能，门槛抬到「天级」：
     * {@link #getRequiredLevel()} = <b>9</b> —— 绝对进化等级 9，也就是 {@code EvolutionTier.TIAN}。
     * <p>
     * 这一条同时管住技能树与自由角色（凡人 / 尸兄）的「发现」：
     * {@code BalanceRules.discoveryLevel} 取 requiredLevel 与 9 的较大值，
     * 所以天级之前连随机机遇都刷不出这条技能。
     */
    @Override
    public int getRequiredLevel() {
        return 9;
    }

    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p) {
        return null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var t=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,20);if(t==null){xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.emptyCast(p);return;}
        var level=(net.minecraft.server.level.ServerLevel)p.level();
        // 水球轨迹：整条直线塌缩成一朵气团（取中点、半径≈线长一半，上限 8）
        var eye=p.getEyePosition();var end=t.getEyePosition();
        double half=Math.min(8.0,eye.distanceTo(end)*.5);
        xiaoshi2022.corpseorigin.skill.chapter.QiEffects.cloud(level,eye.lerp(end,.5),0x4fc3f7,(float)Math.max(.5,half),12);
        t.hurtServer(level,p.damageSources().playerAttack(p),24);
        xiaoshi2022.corpseorigin.skill.chapter.SkillRework.buff(t,net.minecraft.world.effect.MobEffects.SLOWNESS,80,2);
        t.push(p.getLookAngle().x*1.5,.4,p.getLookAngle().z*1.5);t.hurtMarked=true;
    }
}
