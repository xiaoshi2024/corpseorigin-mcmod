package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.growth.RealmNetworking;
import xiaoshi2022.corpseorigin.growth.RealmProgression;
import xiaoshi2022.corpseorigin.growth.RealmRules;
import java.util.*;

/** Paginated layout stays usable on small windows; every mutation is validated on the server. */
public final class RealmGrowthScreen extends Screen {
    private static CompoundTag state=new CompoundTag();

    private final Screen parent;
    private int index;
    private final List<Button> purchases=new ArrayList<>();
    public RealmGrowthScreen(Screen parent){super(Component.translatable("realm.corpseorigin.title"));this.parent=parent;}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(RealmNetworking.State.TYPE,(payload,context)->context.client().execute(()->{
            state=payload.data()==null?new CompoundTag():payload.data().copy();
            if(payload.open()) context.client().gui.setScreen(new RealmGrowthScreen(context.client().gui.screen()));
        }));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{
            state=new CompoundTag();ClientState.evolutionLevel=1;ClientState.pointsToNextLevel=0;
        });
    }
    public static void open(){ClientPlayNetworking.send(new RealmNetworking.Action("view","",0));}
    private void send(String action,String stat,int count){ClientPlayNetworking.send(new RealmNetworking.Action(action,stat,count));}
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.translatable(label),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        int x=width/2-145; purchases.clear();
        button("realm.corpseorigin.button.previous",x,30,85,()->index=Math.floorMod(index-1,6));
        button("realm.corpseorigin.button.next",x+90,30,85,()->index=(index+1)%6);
        button("realm.corpseorigin.button.back",x+180,30,110,this::onClose);
        for(int i=0;i<3;i++){
            int count=new int[]{1,10,100}[i];
            purchases.add(addRenderableWidget(Button.builder(Component.translatable("realm.corpseorigin.train",count),b->send("train",RealmProgression.STATS.get(index),count)).bounds(x+i*98,112,94,20).build()));
        }
        button("realm.corpseorigin.button.meditate",x,140,142,()->{send("meditate","",0);minecraft.gui.setScreen(null);});
        button("realm.corpseorigin.button.recharge",x+148,140,142,()->send("recharge","",0));
        button("realm.corpseorigin.button.blood",x,166,94,()->send("refine","blood",0));
        button("realm.corpseorigin.button.qi",x+98,166,94,()->send("refine","qi",0));
        button("realm.corpseorigin.button.medicine",x+196,166,94,()->send("refine","medicine",0));
        button("realm.corpseorigin.button.burst",x,192,290,()->{send("burst","",0);minecraft.gui.setScreen(null);});
    }
    @Override public void tick(){
        int paid=state.getIntOr("rank_"+RealmProgression.STATS.get(index),0),total=state.getIntOr("total_"+RealmProgression.STATS.get(index),0);
        for(int i=0;i<purchases.size();i++){
            int count=new int[]{1,10,100}[i];long cost=RealmRules.cost(paid,count,state.getIntOr("base_cost",10),state.getIntOr("step_cost",2));
            purchases.get(i).active=state.getBooleanOr("enabled",false) && minecraft.player!=null && !minecraft.player.isCreative()
                    && total+count<=state.getIntOr("rank_limit",0) && cost<=state.getIntOr("available",0);
            purchases.get(i).setMessage(Component.translatable("realm.corpseorigin.price",count,cost));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){
        super.extractRenderState(g,mx,my,pt);int x=width/2-145;String stat=RealmProgression.STATS.get(index);
        g.centeredText(font,title,width/2,12,0xffffffff);
        g.text(font,Component.translatable("realm.corpseorigin.stat."+stat).append("   "+(index+1)+"/6"),x,58,0xffffd77f,false);
        g.text(font,Component.translatable("realm.corpseorigin.panel_balance",state.getIntOr("available",0),state.getIntOr("rank_"+stat,0),state.getIntOr("total_"+stat,0)),x,74,0xffffffff,false);
        g.text(font,Component.translatable("realm.corpseorigin.panel_practice",state.getLongOr("xp_"+stat,0),state.getIntOr("xp_per_rank",100)),x,90,0xffaaccff,false);
        g.text(font,Component.translatable("realm.corpseorigin.panel_materials"),x,214,0xffcccccc,false);
        g.text(font,Component.translatable("realm.corpseorigin.panel_restore",state.getIntOr("recharge_cost",50)),x,226,0xffcccccc,false);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
