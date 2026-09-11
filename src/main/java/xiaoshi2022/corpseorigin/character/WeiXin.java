package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.weixin.BloodCloudSkill;
import xiaoshi2022.corpseorigin.skill.weixin.BloodLotusSkill;

import java.util.List;

/**
 * 唯欣（小萌）- 血莲教圣女
 * <p>
 * 体内有两个人格：妹妹（脆弱、纯洁）与姐姐（残忍、好战）。
 * 拥有将近一个甲子（六十年）的功力。
 */
public class WeiXin implements ICharacter {

    public static final String ID = "weixin";

    private static final List<ISkill> SKILLS = List.of(
            new BloodCloudSkill(),
            new BloodLotusSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.weixin");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.weixin.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("weixin");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.weixin.trait1"),
                Component.translatable("character.corpseorigin.weixin.trait2"),
                Component.translatable("character.corpseorigin.weixin.trait3")
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