package xiaoshi2022.corpseorigin.skill.tushu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.SkillRework;

/** Server-authoritative skill; stable ID retained for existing saves. */
public class DefenseStanceSkill extends AbstractSkill {
 public static final String PATH="defense_stance";
 public DefenseStanceSkill(){super(PATH,SkillType.DEFENSE,300);}

 @Override public void onActivate(ServerPlayer p){SkillRework.buff(p,MobEffects.RESISTANCE,200,3);ChapterCombat.ring((ServerLevel)p.level(),p.position(),1.2,0xf1cf78,40);}
}
