package xiaoshi2022.corpseorigin.skill.heixiaofei;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class RoundDanceSkill extends AbstractSkill {
 public static final String PATH="round_dance";
 public RoundDanceSkill(){super(PATH,SkillType.COMBAT,400);}

 @Override public void onActivate(ServerPlayer p){SkillRework.start(p,"round_dance",40);p.setDeltaMovement(p.getLookAngle().scale(.5).add(0,.65,0));p.hurtMarked=true;ChapterScenes.action(p,"round_dance",40);}
}
