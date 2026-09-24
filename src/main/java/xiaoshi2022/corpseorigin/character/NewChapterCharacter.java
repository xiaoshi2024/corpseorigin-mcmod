package xiaoshi2022.corpseorigin.character;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.NewChapterSkill;
public final class NewChapterCharacter implements ICharacter {
    private final String id;private final boolean corpse;
    public NewChapterCharacter(String id,boolean corpse){this.id=id;this.corpse=corpse;}
    public String getId(){return id;}public Component getName(){return Component.translatable("character.corpseorigin."+id);}
    public Component getDescription(){return Component.translatable(id.equals("xiaojingang")?"character.corpseorigin.xiaojingang.desc":"character.corpseorigin.chapter_placeholder.desc");}
    public Identifier getIcon(){return Identifier.fromNamespaceAndPath("minecraft","textures/item/book.png");}
    public boolean isPassive(){return false;}
    public List<Component> getTraits(){return List.of(Component.translatable(corpse?"character.corpseorigin.form.corpse":"character.corpseorigin.form.human"),getDescription());}
    public int getMaxInnerPower(){return id.equals("guigun_human")?80:0;}
    public List<ISkill> getSkills(){return switch(id){
        case "xiaojingang"->List.of(new NewChapterSkill("gourd_link",20,0,0),
            new NewChapterSkill("gourd_arms",120,15,2),
            new NewChapterSkill("gourd_acid",160,20,4),
            new NewChapterSkill("gourd_devour",200,0,3),
            new NewChapterSkill("gourd_fire",240,30,5),
            new NewChapterSkill("gourd_eyes",240,10,1),
            new NewChapterSkill("gourd_power",300,20,6));
        case "guigun_human"->List.of(stub("guigun_sweep"),stub("guigun_guard"));
        case "guigun_corpse"->List.of(stub("guigun_resonance"),stub("guigun_crush"));
        case "hei_wuchou"->List.of(stub("wuchou_blade"),stub("wuchou_step"));
        default->List.of(stub("wusheng_twin"),stub("wusheng_cross"));};}
    private ISkill stub(String path){return new NewChapterSkill(path,0,0,-1);}
    @Override public void onAcquire(Player p){if(corpse){xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.setPlayerAsCorpse(p,xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.TYPE_ELITE,xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.get(p).restoreConsciousness();if(p instanceof net.minecraft.server.level.ServerPlayer sp)xiaoshi2022.corpseorigin.network.CorpseNetwork.broadcastPlayerCorpseSync(sp);}}
    @Override public void onLose(Player p){if(corpse)xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.removeCorpseState(p);}
}
