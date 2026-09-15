package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.muxi.FiveElementsFormationSkill;
import xiaoshi2022.corpseorigin.skill.muxi.WoodBindSkill;

import java.util.List;

/**
 * 木犀 - 特能二队的五行阵核心，团队增益与区域控制角色
 */
public class MuXi implements ICharacter {

    public static final String ID = "muxi";

    private static final List<ISkill> SKILLS = List.of(
            new FiveElementsFormationSkill(),
            new WoodBindSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin." + ID);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin." + ID + ".desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId(ID);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin." + ID + ".trait1"),
                Component.translatable("character.corpseorigin." + ID + ".trait2"),
                Component.translatable("character.corpseorigin." + ID + ".trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.5f;
    }
}
