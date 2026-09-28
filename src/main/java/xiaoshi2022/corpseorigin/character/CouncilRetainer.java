package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.BlackFridaySkill;
import xiaoshi2022.corpseorigin.skill.chapter.SwordFlowerSkill;

import java.util.List;

/** Independent selectable retainers; the legacy combined identity remains loadable. */
public class CouncilRetainer implements ICharacter {
    private final String id;
    private final List<ISkill> skills;
    public CouncilRetainer(boolean laura) {
        id=laura?"laura":"jack";
        skills=List.of(laura?new SwordFlowerSkill():new BlackFridaySkill());
    }
    @Override public String getId(){return id;}
    @Override public Component getName(){return Component.translatable("character.corpseorigin."+id);}
    @Override public Component getDescription(){return Component.translatable("character.corpseorigin."+id+".desc");}
    @Override public Identifier getIcon(){return ICharacter.iconId("heianhui_suicong");}
    @Override public boolean isPassive(){return false;}
    @Override public List<ISkill> getSkills(){return skills;}
    @Override public List<Component> getTraits(){return List.of();}
}
