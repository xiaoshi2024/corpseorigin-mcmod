package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.fengmohuitailang.MeteorSwordSkill;
import xiaoshi2022.corpseorigin.skill.fengmohuitailang.TenguDivineArraySkill;

import java.util.List;

/**
 * 风魔灰太郎 - 东瀛忍者首领，克制不死系的外敌核心 BOSS
 */
public class FengMoHuiTaiLang implements ICharacter {

    public static final String ID = "fengmohuitailang";

    private static final List<ISkill> SKILLS = List.of(
            new TenguDivineArraySkill(),
            new MeteorSwordSkill()
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
        return 0.3f;
    }
}
