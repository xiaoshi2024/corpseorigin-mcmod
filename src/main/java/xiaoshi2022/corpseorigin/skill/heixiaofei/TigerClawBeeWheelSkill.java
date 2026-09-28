package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

public class TigerClawBeeWheelSkill extends AbstractSkill {
 public static final String PATH="tiger_claw_bee_wheel";
 public TigerClawBeeWheelSkill(){super(PATH,SkillType.COMBAT,100);}
 @Override public Component checkUsable(ServerPlayer p){return p.getMainHandItem().is(ModItems.BEE_WHEEL)?null:Component.translatable("message.corpseorigin.tiger_claw_bee_wheel_skill.text_01");}
 @Override public void onActivate(ServerPlayer p){xiaoshi2022.corpseorigin.item.BeeWheelItem.throwWheel(p);}
}
