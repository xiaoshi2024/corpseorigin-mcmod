package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import java.util.List;

/** Ninefold canon: wisdom and qi are passive foundations; advanced arts require Shen. */
public final class TianGangSkill extends AbstractSkill {
    public enum Form {
        ZHI(0,0), JI(600,15), LI(600,20),
        YU(600,20), QI(0,0), HUI(800,30),
        MIE(300,25), WU(400,25), SHEN(1200,50),
        NIPO(200,15), POGANG(300,20), TIANGANGPO(1000,40);
        final int cooldown,cost;
        Form(int cooldown,int cost){this.cooldown=cooldown;this.cost=cost;}
        public String path(){return "tiangang_"+name().toLowerCase(java.util.Locale.ROOT);}
        public boolean advanced(){return ordinal()>SHEN.ordinal();}
    }
    private final Form form;
    public TianGangSkill(Form form){super(form.path(),form==Form.YU?SkillType.DEFENSE:SkillType.COMBAT,
            form.cooldown,1,1,form!=Form.ZHI && form!=Form.QI,form.cost);this.form=form;}
    @Override public Component getName(){return Component.translatable("skill.corpseorigin."+form.path());}
    @Override public Component getDescription(){return Component.translatable("skill.corpseorigin."+form.path()+".desc");}
    @Override public List<Identifier> getPrerequisites(){
        if(form.advanced())return List.of(CorpseOrigin.id(Form.SHEN.path()));
        if(form==Form.SHEN)return java.util.Arrays.stream(Form.values()).limit(8).map(f->CorpseOrigin.id(f.path())).toList();
        return List.of();
    }
    @Override public Component checkUsable(ServerPlayer p){
        if(form==Form.SHEN){
            var data=xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p);
            for(Form foundation:Form.values())if(foundation.ordinal()<8 && !data.hasLearned(p.getUUID(),foundation.path()))
                return Component.translatable("message.corpseorigin.tian_gang_skill.text_01");
        }
        if(form.advanced() && !TianGangCombat.isShen(p))return Component.translatable("message.corpseorigin.tian_gang_skill.text_02");
        if((form==Form.HUI || form==Form.TIANGANGPO) && (!p.onGround() || !TianGangCombat.canLeap(p)))
            return Component.translatable("message.corpseorigin.tian_gang_skill.text_03");
        return null;
    }
    @Override public void onActivate(ServerPlayer p){TianGangCombat.cast(p,form);}
}
