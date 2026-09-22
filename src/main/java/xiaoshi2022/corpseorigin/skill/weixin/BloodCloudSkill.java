package xiaoshi2022.corpseorigin.skill.weixin;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class BloodCloudSkill extends AbstractSkill {
 public static final String PATH="blood_cloud";
 public BloodCloudSkill(){super(PATH,SkillType.COMBAT,400);}

 @Override public void onActivate(ServerPlayer p){SkillRework.start(p,"blood_cloud",160);p.setDeltaMovement(p.getLookAngle().scale(1.4).add(0,.15,0));p.hurtMarked=true;}
}
