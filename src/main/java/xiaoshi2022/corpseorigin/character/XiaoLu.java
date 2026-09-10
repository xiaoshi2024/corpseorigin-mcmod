package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.xiaolu.OsmiumGoldSkill;
import xiaoshi2022.corpseorigin.skill.xiaolu.OsmiumIceSpikeSkill;

import java.util.List;

/**
 * 小鹿 - 灵鹿化身的辅助/净化角色
 */
public class XiaoLu implements ICharacter {

    public static final String ID = "xiaolu";

    private static final List<ISkill> SKILLS = List.of(
            new OsmiumGoldSkill(),
            new OsmiumIceSpikeSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.xiaolu");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.xiaolu.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("xiaolu");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.xiaolu.trait1"),
                Component.translatable("character.corpseorigin.xiaolu.trait2")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.7f;
    }
}