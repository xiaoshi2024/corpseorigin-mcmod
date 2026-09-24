package xiaoshi2022.corpseorigin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.growth.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** A paged, server-authoritative ladder for each registered organ. */
public final class OrganEvolutionScreen extends Screen {
    private final Screen parent;
    private int index;
    public OrganEvolutionScreen(Screen parent){super(Component.literal("器官进化天梯"));this.parent=parent;}
    private java.util.List<OrganDefinition> entries(){return OrganEvolution.entries(OrganClient.catalog);}
    private void button(String name,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(name),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        int x=width/2-150;
        button("上一器官",x,28,95,()->{index=Math.floorMod(index-1,entries().size());});
        button("下一器官",x+100,28,95,()->{index=(index+1)%entries().size();});
        button("返回装配",x+200,28,100,this::onClose);
        button("解锁 / 进阶",x,160,300,()->send("advance"));
        button("活性 +1",x,185,145,()->send("vitality"));
        button("效率 +1",x+155,185,145,()->send("efficiency"));
        button("选择爆发特化",x,210,145,()->send("power"));
        button("选择续航特化",x+155,210,145,()->send("sustain"));
    }
    private void send(String action){
        if(OrganClient.pending)return;
        OrganClient.pending=true;OrganClient.status="等待服务器确认…";
        ClientPlayNetworking.send(new OrganEvolutionPayload(entries().get(index).id(),action));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractRenderState(g,mx,my,partial);
        if(minecraft==null||minecraft.player==null)return;
        var p=minecraft.player;var def=entries().get(index);int stage=OrganEvolution.stage(p,def.id());int x=width/2-150;
        g.centeredText(font,title,width/2,10,0xffffffff);
        String[] lines={
            (index+1)+" / "+entries().size()+"  "+def.name()+" · "+OrganEvolutionRules.stageName(stage),
            "等级 "+xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(ClientState.earnedPoints)+" · 可用点 "+ClientState.availablePoints+ (p.isCreative()?" · 创造免费":" · 与技能共用"),
            (stage>=1?"✓ ":"□ ")+"I 解锁：5点，等级1 → 可装配 / 对应能力",
            (stage>=2?"✓ ":"□ ")+"II 强化：10点，等级5 → 属性加点",
            (stage>=3?"✓ ":"□ ")+"III 特化：20点，等级9 → 爆发 / 续航二选一",
            "活性 "+OrganEvolution.stat(p,def.id(),"vitality")+" · 效率 "+OrganEvolution.stat(p,def.id(),"efficiency")+" · 特化 "+(OrganEvolution.special(p,def.id()).isEmpty()?"未选":OrganEvolution.special(p,def.id()).equals("power")?"爆发":"续航"),
            "属性下一点：活性 "+OrganEvolutionRules.attributeCost(OrganEvolution.stat(p,def.id(),"vitality"))+" / 效率 "+OrganEvolutionRules.attributeCost(OrganEvolution.stat(p,def.id(),"efficiency"))+" 点"
        };
        for(int i=0;i<lines.length;i++)g.text(font,lines[i],x,55+i*14,0xffdddddd,false);
        g.text(font,"生存每项上限5；同类取最高，不重复叠加",x,239,0xffaaaaaa,false);
        g.text(font,font.plainSubstrByWidth(OrganClient.status,300),x,253,0xffffff55,false);
        if(my>=239)g.setComponentTooltipForNextFrame(font,java.util.List.of(
            Component.literal("活性：每点+2最大生命；效率：每点+0.4护甲，翼/尾每2点降低1/秒耗血，最低1。"),
            Component.literal("爆发：+2攻击，翼/尾冲刺推进，吸血比例15%→20%且上限2→3。"),
            Component.literal("续航：+4最大生命，翼/尾效率额外+2。同类属性取最高；不重复叠加。"),
            Component.literal("属性需先解锁该器官；生存特化不可反复切换。创造可免费反复选择。"),
            Component.literal(OrganClient.status)),mx,my);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
