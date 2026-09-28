package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.SkillRework;

/** Server-authoritative skill; stable ID retained for existing saves. */
public class BloodLotusArmorSkill extends AbstractSkill {
 public static final String PATH="blood_lotus_armor";
 public BloodLotusArmorSkill(){super(PATH,SkillType.DEFENSE,600);}

 @Override public void onActivate(ServerPlayer p){p.setAttached(SkillRework.LOTUS_ARMOR,p.level().getGameTime()+300);SkillRework.buff(p,MobEffects.RESISTANCE,300,3);SkillRework.buff(p,MobEffects.ABSORPTION,300,3);}
}
