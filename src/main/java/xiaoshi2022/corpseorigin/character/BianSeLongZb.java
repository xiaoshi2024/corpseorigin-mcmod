package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.bianselong_zb.ChameleonDisguiseSkill;
import xiaoshi2022.corpseorigin.skill.bianselong_zb.HeartGrabAmbushSkill;

import java.util.List;

/**
 * 变色龙尸兄 - 尸巢篇关键偷袭角色，可伪装成小惠潜入并捏心暗杀。
 */
public class BianSeLongZb implements ICharacter {

    public static final String ID = "bianselong_zb";

    private static final List<ISkill> SKILLS = List.of(
            new ChameleonDisguiseSkill(),
            new HeartGrabAmbushSkill()
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
