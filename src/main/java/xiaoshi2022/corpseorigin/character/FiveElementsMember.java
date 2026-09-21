package xiaoshi2022.corpseorigin.character;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import java.util.List;
/** Unnamed members deliberately remain element-labelled placeholders. */
public class FiveElementsMember implements ICharacter {
    private final String element;
    public FiveElementsMember(String element){this.element=element;}
    @Override public String getId(){return "formation_"+element;}
    @Override public Component getName(){return Component.translatable("character.corpseorigin."+getId());}
    @Override public Component getDescription(){return Component.translatable("character.corpseorigin.formation_member.desc");}
    @Override public Identifier getIcon(){return ICharacter.iconId("muxi");}
    @Override public boolean isPassive(){return false;}
    @Override public List<Component> getTraits(){return List.of();}
    @Override public int getMaxInnerPower(){return 80;}
    @Override public List<ISkill> getSkills(){return List.of(new xiaoshi2022.corpseorigin.skill.muxi.FiveElementsFormationSkill());}
}
