package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.HeavySmashSkill;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.LifeDrainSuckSkill;

import java.util.List;

/**
 * 天线宝宝尸兄 - 尸巢篇开篇小 BOSS，靠吸食与耳目虫传讯压迫玩家。
 */
public class TianXianBaoBaoZb implements ICharacter {

    public static final String ID = "tianxianbaobao_zb";

    private static final List<ISkill> SKILLS = List.of(
            new LifeDrainSuckSkill(),
            new HeavySmashSkill()
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
