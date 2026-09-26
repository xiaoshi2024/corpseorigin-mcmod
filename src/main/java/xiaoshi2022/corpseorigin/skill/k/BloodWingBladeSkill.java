package xiaoshi2022.corpseorigin.skill.k;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 血翼黑刃：主手右键或技能栏发射血光剑气，消耗10点内力。
 * 冷却：10 秒（200 ticks）。
 * 命中按实际伤害吸血；武器使用与技能栏共用服务器校验和冷却。
 */
public class BloodWingBladeSkill extends AbstractSkill {

    public static final String PATH = "blood_wing_blade";

    public BloodWingBladeSkill() {
        super(PATH, SkillType.COMBAT, 200, 10);
    }
    @Override public net.minecraft.network.chat.Component checkUsable(net.minecraft.server.level.ServerPlayer player) {
        if (!player.getMainHandItem().is(xiaoshi2022.corpseorigin.registry.ModItems.BLOOD_WING_BLADE))
            return net.minecraft.network.chat.Component.translatable("skill.corpseorigin.blood_wing_blade.need_weapon");
        return null;
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer player) {
        var beam = new xiaoshi2022.corpseorigin.entity.BloodWingBeamEntity(
                xiaoshi2022.corpseorigin.registry.ModEntities.BLOOD_WING_BEAM, player.level());
        beam.setOwner(player);
        beam.setPos(player.getEyePosition().add(0, -.1, 0));
        beam.setDamage((float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.5f);
        beam.setLevel(4); // 16 ticks at 1.5 blocks/tick = 24 blocks.
        beam.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, beam.getVelocity(), 0);
        player.level().addFreshEntity(beam);
        player.getMainHandItem().hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
}
