package xiaoshi2022.corpseorigin.skill.weixin;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
public class BloodLotusSkill extends AbstractSkill {
 public BloodLotusSkill(){super("blood_lotus",SkillType.ULTIMATE,1200);}
 @Override public void onActivate(ServerPlayer p){
 var petal=SkillConstructEntity.spawn(p,ModEntities.BLOOD_LOTUS_PETAL,null,60);
 petal.setDeltaMovement(p.getLookAngle().scale(1.1));petal.hurtMarked=true;
 }
}
