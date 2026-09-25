package xiaoshi2022.corpseorigin.skill.bianyi_guiyu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

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
        return null;
    }
    @Override
    public void onActivate(ServerPlayer p) {
        var target = ChapterCombat.aim(p, 4);
        if (target == null) { ChapterCombat.emptyCast(p); return; }
        boolean inWater = p.isInWater();
        p.setAttached(ChapterActorState.BITE_UNTIL, p.level().getGameTime() + 12);
        target.hurtServer((ServerLevel) p.level(), p.damageSources().playerAttack(p), inWater ? 8 : 5);
        p.setDeltaMovement(p.getLookAngle().scale(inWater ? .8 : .4));
        p.hurtMarked = true;
        QiEffects.burst((ServerLevel) p.level(), p.getX(), p.getY() + .5, p.getZ(),
                0x4fc3f7, inWater ? 20 : 8, .5);
    }
}
