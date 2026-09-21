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
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p) {
        return xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,4)==null
                ? net.minecraft.network.chat.Component.translatable("skill.corpseorigin.chapter.need_target") : null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,4); if(target==null)return;
        boolean story=!(target instanceof net.minecraft.world.entity.player.Player) && target.entityTags().contains("corpseorigin_story_execution");
        target.hurtServer((net.minecraft.server.level.ServerLevel)p.level(),p.damageSources().playerAttack(p),story?target.getHealth()*20+100:12);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        ((net.minecraft.server.level.ServerLevel)p.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                target.getX(),target.getY()+1,target.getZ(),1,0,0,0,0);
    }
}
