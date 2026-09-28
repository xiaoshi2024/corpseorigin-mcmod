package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.growth.ThermalSurvey;
import xiaoshi2022.corpseorigin.network.ThermalSurveyPayload;

public final class ThermalHudOverlay {
    private static ThermalSurveyPayload state;
    private ThermalHudOverlay(){}
    public static ThermalSurveyPayload currentState(){return state;}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(ThermalSurveyPayload.TYPE,(p,c)->c.client().execute(()->state=p.active()?p:null));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->state=null);
        HudElementRegistry.addLast(CorpseOrigin.id("thermal_hud"),(g,delta)->{
            var mc=Minecraft.getInstance();
            if(mc.player==null||mc.level==null||!mc.player.isAlive()||mc.player.isSpectator()
                    ||mc.gui.hud.isHidden()||!ClientState.hudVisible||!ThermalSurvey.carriesScanner(mc.player))return;
            var s=state;
            boolean ready=s!=null&&s.dimension().equals(mc.level.dimension().identifier().toString());
            java.util.List<Component> lines=new java.util.ArrayList<>();
            lines.add(Component.translatable("thermal.corpseorigin.hud.title"));
            if(ready){
                var r=s.result();
                lines.add(Component.translatable("thermal.corpseorigin.hud.corpses",r.corpses(),r.corpsePlayers()));
                lines.add(Component.translatable("thermal.corpseorigin.hud.infected",r.incubating()));
                lines.add(Component.translatable("thermal.corpseorigin.hud.chunks",r.loaded(),r.explored()));
                lines.add(Component.translatable("thermal.corpseorigin.hud.unknown",r.unloaded()));
            }else lines.add(Component.translatable("thermal.corpseorigin.hud.scanning"));
            int width=Math.min(g.guiWidth()-16,lines.stream().mapToInt(mc.font::width).max().orElse(140)+12);
            int x=8,y=8,step=mc.font.lineHeight+3;
            g.fill(x,y,x+width,y+lines.size()*step+10,0xC0102020);
            g.fill(x,y,x+2,y+lines.size()*step+10,0xFF60FFD0);
            for(int i=0;i<lines.size();i++)g.text(mc.font,mc.font.plainSubstrByWidth(lines.get(i).getString(),width-12),x+6,y+5+i*step,i==0?0xFF60FFD0:0xFFE0F5EF,true);
        });
    }
}
