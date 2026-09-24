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
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p) {
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,3);
        if(target==null) return null;
        var toAttacker=p.position().subtract(target.position()).normalize();
        return target.getLookAngle().dot(toAttacker)>-.25 ? net.minecraft.network.chat.Component.translatable("skill.corpseorigin.heart_grab_ambush.need_back") : null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,3); if(target==null){xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.emptyCast(p);return;}
        p.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE,"");
        xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.action(p,"ambush",12);
        target.hurtServer((net.minecraft.server.level.ServerLevel)p.level(),p.damageSources().playerAttack(p),14);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        ((net.minecraft.server.level.ServerLevel)p.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                target.getX(),target.getY()+1,target.getZ(),25,.3,.4,.3,.1);
    }
}
