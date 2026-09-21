package xiaoshi2022.corpseorigin.skill.bianyi_guiyu;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 变异鲑鱼·水战撕咬 —— 水下突袭撕咬。
 * <p>
 * 冷却：6 秒（120 ticks）。
 * <p>
 * 特效：水花粒子 + 突袭动画。
 * <p>
 * TODO 实装：水下突进 + 撕咬伤害 + 水花粒子。
 */
public class WaterBiteSkill extends AbstractSkill {

    public static final String PATH = "water_bite";

    public WaterBiteSkill() {
        super(PATH, SkillType.COMBAT, 120);
    }
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p) {
        if(!p.isInWater()) return net.minecraft.network.chat.Component.translatable("skill.corpseorigin.chapter.need_water");
        return xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,4)==null
                ? net.minecraft.network.chat.Component.translatable("skill.corpseorigin.chapter.need_target") : null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,4);
        if(target==null) return;
        p.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.BITE_UNTIL,p.level().getGameTime()+12);
        target.hurtServer((net.minecraft.server.level.ServerLevel)p.level(),p.damageSources().playerAttack(p),8);
        p.setDeltaMovement(p.getLookAngle().scale(.8)); p.hurtMarked=true;
        ((net.minecraft.server.level.ServerLevel)p.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                p.getX(),p.getY()+.5,p.getZ(),20,.5,.3,.5,.03);
    }
}
