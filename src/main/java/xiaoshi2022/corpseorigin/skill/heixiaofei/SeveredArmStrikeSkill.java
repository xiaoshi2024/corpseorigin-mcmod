package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.BodySkillState;

public class SeveredArmStrikeSkill extends AbstractSkill {
 public static final String PATH="severed_arm_strike";
 public SeveredArmStrikeSkill(){super(PATH,SkillType.COMBAT,200);}
 @Override public Component checkUsable(ServerPlayer p){return BodySkillState.hasArm(p)?null:Component.translatable("message.corpseorigin.severed_arm_strike_skill.text_01");}
 @Override public void onActivate(ServerPlayer p){
  if(!BodySkillState.hasArm(p))return;
  p.setAttached(BodySkillState.FOREARM_UNTIL,p.level().getGameTime()+80);
  var arm=SkillConstructEntity.spawn(p,ModEntities.SEVERED_FOREARM,null,60);
  arm.setDeltaMovement(p.getLookAngle().scale(1.6));arm.hurtMarked=true;
  p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
 }
}
