package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.BloodLotusArmorSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.GroundBurrowSkill;

import java.util.List;

/**
 * 尸巢之子 - 少教主，《尸巢之战篇》的最终隐藏 BOSS。
 */
public class ShiChaoZhiZi implements ICharacter {

    public static final String ID = "shichaozhizi";

    private static final List<ISkill> SKILLS = List.of(
            new BloodLotusArmorSkill(),
            new GroundBurrowSkill()
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
        return 0.0f;
    }
}
