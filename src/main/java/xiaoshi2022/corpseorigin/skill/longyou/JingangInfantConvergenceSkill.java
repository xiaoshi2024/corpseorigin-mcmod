package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/** Recall a nearby Jingang infant and connect it to this undead body. */
public final class JingangInfantConvergenceSkill extends AbstractSkill {
    public static final String PATH = "jingang_infant_convergence";
    public JingangInfantConvergenceSkill() { super(PATH, SkillType.UTILITY, 200); }

    @Override public Component checkUsable(ServerPlayer player) {
        return JingangInfantLink.findCandidate(player) == null
                ? Component.translatable("skill.corpseorigin.jingang_infant_convergence.no_target") : null;
    }

    @Override public void onActivate(ServerPlayer player) {
        JingangInfantLink.converge(player);
    }
}
