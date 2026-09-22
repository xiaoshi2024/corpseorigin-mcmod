package xiaoshi2022.corpseorigin.skill.jingang_zb;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.longyou.BodyPossession;

public final class PeelShellSkill extends AbstractSkill {
    public PeelShellSkill() { super("peel_shell", SkillType.UTILITY, 20); }
    @Override public void onActivate(ServerPlayer player) { BodyPossession.peel(player); }
}
