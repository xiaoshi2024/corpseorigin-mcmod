package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/** 尸王召集受威压的尸族，通过互相吞噬建造尸巢。 */
public final class CorpseBrotherRallySkill extends AbstractSkill {
    public static final String PATH = "corpse_brother_rally";

    public CorpseBrotherRallySkill() {
        super(PATH, SkillType.ULTIMATE, 1200);
    }

    @Override public int getCost() { return 100; }
    @Override public int getRequiredLevel() { return 5; }

    @Override
    public Component checkUsable(ServerPlayer player) {
        return CorpseNestConstructionHandler.canStart(player);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        CorpseNestConstructionHandler.start(player);
    }
}
