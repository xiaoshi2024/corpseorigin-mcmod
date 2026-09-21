package xiaoshi2022.corpseorigin.skill.yanyan;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
public class FlameStrikeSkill extends AbstractSkill {
    public FlameStrikeSkill() { super("flame_strike", SkillType.COMBAT, 100, 12); }
    @Override public Component checkUsable(ServerPlayer p) {
        return ChapterCombat.aim(p,8)==null ? Component.translatable("skill.corpseorigin.chapter.need_target") : null;
    }
    @Override public void onActivate(ServerPlayer p) {
        var target=ChapterCombat.aim(p,8); if(target!=null) FiveElementsCombat.strike(p,target);
    }
}
