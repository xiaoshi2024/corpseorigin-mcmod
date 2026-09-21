package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

public final class NestSenseSkill extends AbstractSkill {
    public static final String PATH = "nest_sense";
    public NestSenseSkill() { super(PATH, SkillType.DEFENSE, 0); }
    @Override public void onActivate(ServerPlayer player) { NestDefense.open(player); }
}
