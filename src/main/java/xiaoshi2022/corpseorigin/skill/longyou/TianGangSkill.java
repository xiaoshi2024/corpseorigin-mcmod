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
        ZHI("天罡气一重·智",0,0), JI("天罡气二重·疾",600,15), LI("天罡气三重·力",600,20),
        YU("天罡气四重·御",600,20), QI("天罡气五重·气",0,0), HUI("天罡气六重·毁",800,30),
        MIE("天罡气七重·灭",300,25), WU("天罡气八重·无",400,25), SHEN("天罡气九重·神",1200,50),
        NIPO("逆破拳",200,15), POGANG("破罡",300,20), TIANGANGPO("天罡破",1000,40);
        final String title;final int cooldown,cost;
        Form(String title,int cooldown,int cost){this.title=title;this.cooldown=cooldown;this.cost=cost;}
        public String path(){return "tiangang_"+name().toLowerCase(java.util.Locale.ROOT);}
        public boolean advanced(){return ordinal()>SHEN.ordinal();}
    }
    private final Form form;
    public TianGangSkill(Form form){super(form.path(),form==Form.YU?SkillType.DEFENSE:SkillType.COMBAT,
            form.cooldown,1,1,form!=Form.ZHI && form!=Form.QI,form.cost);this.form=form;}
    @Override public Component getName(){return Component.literal(form.title);}
    @Override public Component getDescription(){
        return Component.literal(switch(form){
            case ZHI,QI -> "合一基础：不单独施放，参与九重·神的八重合一。";
            case JI -> "30秒内速度大幅提升。";
            case LI -> "30秒内身体缩小至75%，力量大幅提升。";
            case YU -> "内力护体15秒，物理和内力攻击减伤80%，完全化解天罡匙激光的伤害与击退。";
            case HUI -> "跃起后向下猛击，落地释放8格震地冲击。";
            case MIE -> "右拳打出红色冲击波，沿途冲击敌人。";
            case WU -> "左拳破除目标的护盾、抗性、吸收与血莲气甲。";
            case SHEN -> "习得前八重后八重合一，30秒强化攻防速度，完全化解天罡匙激光，开放高级招式。";
            case NIPO -> "九重·神期间连续五拳打击前方近身目标。";
            case POGANG -> "九重·神期间击破近身目标的内力防御。";
            case TIANGANGPO -> "九重·神期间跃起下砸，释放12格强冲击和地面破坏。";
        });
    }
    @Override public List<Identifier> getPrerequisites(){
        if(form.advanced())return List.of(CorpseOrigin.id(Form.SHEN.path()));
        if(form==Form.SHEN)return java.util.Arrays.stream(Form.values()).limit(8).map(f->CorpseOrigin.id(f.path())).toList();
        return List.of();
    }
    @Override public Component checkUsable(ServerPlayer p){
        if(form==Form.SHEN){
            var data=xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p);
            for(Form foundation:Form.values())if(foundation.ordinal()<8 && !data.hasLearned(p.getUUID(),foundation.path()))
                return Component.literal("九重·神需要先习得前八重功法。");
        }
        if(form.advanced() && !TianGangCombat.isShen(p))return Component.literal("需先开启天罡气九重·神。");
        if((form==Form.HUI || form==Form.TIANGANGPO) && (!p.onGround() || !TianGangCombat.canLeap(p)))
            return Component.literal("需要站稳，且头顶有足够空间起跳。");
        return null;
    }
    @Override public void onActivate(ServerPlayer p){TianGangCombat.cast(p,form);}
}
