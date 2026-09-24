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
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p) {
        return null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        var t=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,20);if(t==null){xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.emptyCast(p);return;}
        var level=(net.minecraft.server.level.ServerLevel)p.level();
        for(int i=0;i<24;i++) {
            var v=p.getEyePosition().lerp(t.getEyePosition(),i/24.0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,v.x,v.y,v.z,5,.15,.15,.15,.1);
        }
        t.hurtServer(level,p.damageSources().playerAttack(p),24);
        xiaoshi2022.corpseorigin.skill.chapter.SkillRework.buff(t,net.minecraft.world.effect.MobEffects.SLOWNESS,80,2);
        t.push(p.getLookAngle().x*1.5,.4,p.getLookAngle().z*1.5);t.hurtMarked=true;
    }
}
