package xiaoshi2022.corpseorigin.skill.fengmohuitailang;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 风魔灰太郎·红陨石之剑 —— 陨石铸剑，对尸王特攻。
 * <p>
 * 设定效果：挥动陨石所铸之剑释放红色剑气，对尸王/尸兄类目标造成额外伤害。
 * 冷却：30 秒（600 ticks）。
 * 特效：红陨石剑气。
 * <p>
 * TODO 实装：挥剑释放红色剑气 + 对尸王/尸兄类目标额外伤害。
 */
public class MeteorSwordSkill extends AbstractSkill {

    public static final String PATH = "meteor_sword";

    public MeteorSwordSkill() {
        super(PATH, SkillType.COMBAT, 600);
    }
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer p){
        if(!p.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.RED_METEOR_SWORD))return net.minecraft.network.chat.Component.translatable("skill.corpseorigin.meteor_sword.need_weapon");
        return xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,12)==null?net.minecraft.network.chat.Component.translatable("skill.corpseorigin.chapter.need_target"):null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p){
        var target=xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.aim(p,12);if(target==null)return;
        var level=(net.minecraft.server.level.ServerLevel)p.level();
        for(int i=0;i<24;i++)xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.dust(level,p.getEyePosition().lerp(target.getEyePosition(),i/24.0),0xeb3349,1.5f);
        target.hurtServer(level,p.damageSources().playerAttack(p),10);
        p.getMainHandItem().hurtAndBreak(1,p,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
