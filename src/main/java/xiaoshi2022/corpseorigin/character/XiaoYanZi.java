package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.xiaoyanzi.EscapePassiveSkill;
import xiaoshi2022.corpseorigin.skill.xiaoyanzi.HamSummonSkill;

import java.util.List;

/**
 * 小言子 - 专属辅助/逃生角色，在尸巢之战中负责保护小鹿。
 */
public class XiaoYanZi implements ICharacter {

    public static final String ID = "xiaoyanzi";

    private static final List<ISkill> SKILLS = List.of(
            new HamSummonSkill(),
            new EscapePassiveSkill()
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
        return 0.8f;
    }
}
