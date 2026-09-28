package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/** Server-authoritative skill; stable ID retained for existing saves. */
public class SwordFlowerSkill extends AbstractSkill {
 public static final String PATH="sword_flower";
 public SwordFlowerSkill(){super(PATH,SkillType.COMBAT,300);}

 @Override public void onActivate(ServerPlayer p){SkillRework.start(p,"sword_flower",160);}
}
