package xiaoshi2022.corpseorigin.skill.longyou;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;
public final class XuanwuBodySkill extends AbstractSkill {
 public XuanwuBodySkill(){super("xuanwu_body",SkillType.DEFENSE,20);}
 @Override public void onActivate(ServerPlayer p){UndeadBodyState.toggle(p);}
}
