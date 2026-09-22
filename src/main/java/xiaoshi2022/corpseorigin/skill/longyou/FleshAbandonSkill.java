package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

public final class FleshAbandonSkill extends AbstractSkill {
    public FleshAbandonSkill() { super("flesh_abandon", SkillType.UTILITY, 200); }

    @Override
    public void onActivate(ServerPlayer player) {
        BodyTransplantHandler.abandonBody(player);
    }
}
