package xiaoshi2022.corpseorigin.skill.yanyan;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.*;
public class SwallowNestSkill extends AbstractSkill {
    public SwallowNestSkill() { super("swallow_nest", SkillType.COMBAT, 120, 8); }
    @Override public Component checkUsable(ServerPlayer p) {
        return ChapterCombat.aim(p,12)==null ? Component.translatable("skill.corpseorigin.chapter.need_target") : null;
    }
    @Override public void onActivate(ServerPlayer p) {
        var target=ChapterCombat.aim(p,12); if(target!=null) FiveElementsCombat.mark(p,target);
    }
}
