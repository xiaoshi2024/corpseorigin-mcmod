package xiaoshi2022.corpseorigin.skill.weixin;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.SkillRework;

/** Server-authoritative skill; stable ID retained for existing saves. */
public class BloodCloudSkill extends AbstractSkill {
 public static final String PATH="blood_cloud";
 public BloodCloudSkill(){super(PATH,SkillType.COMBAT,400);}

 @Override public void onActivate(ServerPlayer p){SkillRework.start(p,"blood_cloud",160);p.setDeltaMovement(p.getLookAngle().scale(1.4).add(0,.15,0));p.hurtMarked=true;}
}
