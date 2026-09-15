package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.List;

/**
 * 黑暗议会随从 - 杰克与劳拉合并后的议会随从杂兵
 */
public class HeiAnHuiSuiCong implements ICharacter {

    public static final String ID = "heianhui_suicong";

    private static final List<ISkill> SKILLS = List.of();

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
        return 0.6f;
    }
}
