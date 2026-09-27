package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.*;

/** Controls are innate; each donor adaptation still requires a successful devour roll. */
public final class GourdInheritanceSkill extends AbstractSkill {
    private final int mode;
    public GourdInheritanceSkill(int mode) {
        super(switch (mode) { case 0 -> "gourd_inheritance_cycle"; case 1 -> "gourd_inheritance"; default -> "gourd_mortal_disguise"; }, SkillType.UTILITY, 10);
        this.mode = mode;
    }
    @Override public SkillResourceRules.Cost getResourceCost() { return new SkillResourceRules.Cost(0, 0); }
    @Override public Component checkUsable(ServerPlayer p) { return GourdInheritance.reason(p, mode); }
    @Override public void onActivate(ServerPlayer p) { GourdInheritance.use(p, mode); }
}
