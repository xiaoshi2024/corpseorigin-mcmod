package xiaoshi2022.corpseorigin.skill.zhaoritian;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

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
        return null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,4); if(target==null){xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.emptyCast(p);return;}
        boolean story=!(target instanceof net.minecraft.world.entity.player.Player) && target.entityTags().contains("corpseorigin_story_execution");
        target.hurtServer((net.minecraft.server.level.ServerLevel)p.level(),p.damageSources().playerAttack(p),story?target.getHealth()*20+100:80);
        xiaoshi2022.corpseorigin.skill.chapter.ImpactTerrain.launch(p,target,3);p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        QiEffects.burst((net.minecraft.server.level.ServerLevel)p.level(),
                target.getX(),target.getY()+1,target.getZ(),0xd8552c,1,0);
    }
}
