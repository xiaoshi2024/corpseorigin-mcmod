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
    public OrganEvolutionScreen(Screen parent){super(Component.translatable("message.corpseorigin.organ_evolution_screen.text_01"));this.parent=parent;}
    private java.util.List<OrganDefinition> entries(){return OrganEvolution.entries(OrganClient.catalog);}
    private void button(String name,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(name),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        int x=width/2-150;
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.064"),x,28,95,()->{index=Math.floorMod(index-1,entries().size());});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.065"),x+100,28,95,()->{index=(index+1)%entries().size();});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.016"),x+200,28,100,this::onClose);
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.066"),x,160,300,()->send("advance"));
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.067"),x,185,145,()->send("vitality"));
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.068"),x+155,185,145,()->send("efficiency"));
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.069"),x,210,145,()->send("power"));
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.070"),x+155,210,145,()->send("sustain"));
    }
    private void send(String action){
        if(OrganClient.pending)return;
        OrganClient.pending=true;OrganClient.status=Component.translatable("gui.corpseorigin.label.045");
        ClientPlayNetworking.send(new OrganEvolutionPayload(entries().get(index).id(),action));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractRenderState(g,mx,my,partial);
        if(minecraft==null||minecraft.player==null)return;
        var p=minecraft.player;var def=entries().get(index);int stage=OrganEvolution.stage(p,def.id());int x=width/2-150;
        g.centeredText(font,title,width/2,10,0xffffffff);
        String[] lines={
            (index+1)+" / "+entries().size()+"  "+def.displayName().getString()+" · "+net.minecraft.client.resources.language.I18n.get(OrganEvolutionRules.stageName(stage)),
            net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.071")+xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(ClientState.earnedPoints)+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.072")+ClientState.availablePoints+ (p.isCreative()?net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.073"):net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.074")),
            (stage>=1?"✓ ":"□ ")+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.075"),
            (stage>=2?"✓ ":"□ ")+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.076"),
            (stage>=3?"✓ ":"□ ")+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.077"),
            net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.078")+OrganEvolution.stat(p,def.id(),"vitality")+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.079")+OrganEvolution.stat(p,def.id(),"efficiency")+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.080")+(OrganEvolution.special(p,def.id()).isEmpty()?net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.081"):OrganEvolution.special(p,def.id()).equals("power")?net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.082"):net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.083")),
            net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.084")+OrganEvolutionRules.attributeCost(OrganEvolution.stat(p,def.id(),"vitality"))+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.085")+OrganEvolutionRules.attributeCost(OrganEvolution.stat(p,def.id(),"efficiency"))+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.086")
        };
        for(int i=0;i<lines.length;i++)g.text(font,lines[i],x,55+i*14,0xffdddddd,false);
        g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.087"),x,239,0xffaaaaaa,false);
        g.text(font,font.plainSubstrByWidth(OrganClient.status.getString(),300),x,253,0xffffff55,false);
        if(my>=239)g.setComponentTooltipForNextFrame(font,java.util.List.of(
            Component.translatable("message.corpseorigin.organ_evolution_screen.text_02"),
            Component.translatable("message.corpseorigin.organ_evolution_screen.text_03"),
            Component.translatable("message.corpseorigin.organ_evolution_screen.text_04"),
            Component.translatable("message.corpseorigin.organ_evolution_screen.text_05"),
            OrganClient.status),mx,my);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
