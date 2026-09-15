package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.baixiaofei.AncientPoetrySwordSkill;
import xiaoshi2022.corpseorigin.skill.baixiaofei.SlaughterAwakeningSkill;
import xiaoshi2022.corpseorigin.skill.baixiaofei.SpatialBlinkSkill;
import xiaoshi2022.corpseorigin.skill.baixiaofei.WaterOrbSkill;

import java.util.List;

/**
 * 白小飞 -（主角）
 * <p>
 * 含《尸巢之战篇》设定的水异能、空间异能骨架技能。
 */
public class BaiXiaoFei implements ICharacter {

    public static final String ID = "baixiaofei";

    private static final List<ISkill> SKILLS = List.of(
            new SlaughterAwakeningSkill(),
            new AncientPoetrySwordSkill(),
            new WaterOrbSkill(),
            new SpatialBlinkSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.baixiaofei");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.baixiaofei.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("baixiaofei");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.baixiaofei.trait1"),
                Component.translatable("character.corpseorigin.baixiaofei.trait2")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.4f;
    }
}