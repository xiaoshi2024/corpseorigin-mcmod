package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 凡人 - 默认角色
 */
public class MortalCharacter implements ICharacter {

    public static final String ID = "mortal";
    private static final MortalCharacter INSTANCE = new MortalCharacter();

    private MortalCharacter() {
    }

    public static MortalCharacter getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.mortal");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.mortal.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("mortal");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.mortal.trait1"),
                Component.translatable("character.corpseorigin.mortal.trait2")
        );
    }
    @Override public List<xiaoshi2022.corpseorigin.skill.ISkill> getSkills() {
        return xiaoshi2022.corpseorigin.growth.FreeGrowth.skills();
    }
}
