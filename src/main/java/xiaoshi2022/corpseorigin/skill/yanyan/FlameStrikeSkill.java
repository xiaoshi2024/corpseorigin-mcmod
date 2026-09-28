package xiaoshi2022.corpseorigin.skill.yanyan;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.FiveElementsCombat;

public class FlameStrikeSkill extends AbstractSkill {
    public FlameStrikeSkill() { super("flame_strike", SkillType.COMBAT, 100, 12); }
    @Override public Component checkUsable(ServerPlayer p) {
        return null;
    }
    @Override public void onActivate(ServerPlayer p) {
        var target=ChapterCombat.aim(p,8); if(target!=null) FiveElementsCombat.strike(p,target); else xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.emptyCast(p);
    }
}
