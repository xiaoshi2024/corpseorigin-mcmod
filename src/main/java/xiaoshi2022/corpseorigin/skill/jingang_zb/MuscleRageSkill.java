package xiaoshi2022.corpseorigin.skill.jingang_zb;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

public final class MuscleRageSkill extends AbstractSkill {
    public MuscleRageSkill(){super("muscle_rage",SkillType.COMBAT,400);}
    @Override public void onActivate(ServerPlayer p){xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities.rage(p);}
}
