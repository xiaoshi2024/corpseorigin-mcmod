package xiaoshi2022.corpseorigin.skill.k;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * K·血翼黑刃 —— 近战血刃攻击并吸取生命。
 * <p>
 * 设定效果：挥出血翼黑刃重击近身目标，并将造成的伤害转化为自身生命回复。
 * 冷却：10 秒（200 ticks）。
 * 特效：血刃武器模型 + 血粒子。
 * <p>
 * TODO 实装：近战重击 + 吸血回复 + 血刃武器模型。
 */
public class BloodWingBladeSkill extends AbstractSkill {

    public static final String PATH = "blood_wing_blade";

    public BloodWingBladeSkill() {
        super(PATH, SkillType.COMBAT, 200, 10);
    }
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer player) {
        if (!player.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.BLOOD_WING_BLADE))
            return net.minecraft.network.chat.Component.translatable("skill.corpseorigin.blood_wing_blade.need_weapon");
        return xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(player,4)==null
                ? net.minecraft.network.chat.Component.translatable("skill.corpseorigin.chapter.need_target") : null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer player) {
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(player,4);
        if(target==null) return;
        float before=target.getHealth();
        if(target.hurtServer((net.minecraft.server.level.ServerLevel)player.level(),player.damageSources().playerAttack(player),10)) {
            player.heal(Math.min(4,Math.max(0,before-target.getHealth())*.4f));
            player.getMainHandItem().hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
