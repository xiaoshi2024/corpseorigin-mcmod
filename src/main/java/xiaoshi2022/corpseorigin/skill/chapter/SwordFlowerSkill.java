package xiaoshi2022.corpseorigin.skill.chapter;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
/** Server-authoritative skill; stable ID retained for existing saves. */
public class SwordFlowerSkill extends AbstractSkill {
 public static final String PATH="sword_flower";
 public SwordFlowerSkill(){super(PATH,SkillType.COMBAT,300);}

 @Override public void onActivate(ServerPlayer p){SkillRework.start(p,"sword_flower",160);}
}
