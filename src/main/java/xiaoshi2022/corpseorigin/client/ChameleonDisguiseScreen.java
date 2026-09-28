package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.network.ChameleonDisguisePayload;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

public final class ChameleonDisguiseScreen extends Screen {
    private boolean other,waiting;
    private EditBox playerId;
    private Button confirm;
    private Component status=Component.empty();
    public ChameleonDisguiseScreen(){super(Component.translatable("gui.corpseorigin.disguise.title"));}
    public static void activate(String skill){
        if(skill.equals("chameleon_disguise"))Minecraft.getInstance().gui.setScreen(new ChameleonDisguiseScreen());
        else ClientPlayNetworking.send(new CorpsePayloads.ActivateSkillC2S(skill));
    }
    @Override protected void init(){
        int w=Math.min(280,width-24),x=(width-w)/2,y=Math.max(26,height/2-70);
        String value=playerId==null?"":playerId.getValue();
        addRenderableWidget(CycleButton.<Boolean>builder(v->Component.translatable(v?"gui.corpseorigin.disguise.player":"gui.corpseorigin.disguise.xiaohui"),other)
                .withValues(false,true).displayOnlyValue().create(x,y,w,20,Component.translatable("gui.corpseorigin.disguise.type"),(button,v)->{
                    other=v;playerId.visible=v;playerId.active=v;status=Component.empty();if(v)setFocused(playerId);
                }));
        playerId=addRenderableWidget(new EditBox(font,x,y+30,w,20,Component.translatable("gui.corpseorigin.disguise.id")));
        playerId.setMaxLength(36);playerId.setValue(value);playerId.setHint(Component.translatable("gui.corpseorigin.disguise.id"));
        playerId.visible=other;playerId.active=other;
        confirm=addRenderableWidget(Button.builder(Component.translatable("gui.corpseorigin.disguise.confirm"),b->{
            waiting=true;confirm.active=false;status=Component.translatable("gui.corpseorigin.disguise.loading");
            ClientPlayNetworking.send(new ChameleonDisguisePayload.Select(other?playerId.getValue().strip():""));
        }).bounds(x,y+98,w/2-3,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(x+w/2+3,y+98,w/2-3,20).build());
        String current = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getAttachedOrCreate(ChapterActorState.DISGUISE)
                : "";
        if (!current.isEmpty()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.corpseorigin.disguise.restore"),
                    b -> {
                        waiting = true;
                        status = Component.translatable("gui.corpseorigin.disguise.loading");
                        ClientPlayNetworking.send(new ChameleonDisguisePayload.Select("#restore"));
                    }).bounds(x, y + 124, w, 20).build());
        }
    }
    public void result(String error){
        if(error.isEmpty()){waiting=false;onClose();return;}
        waiting=false;status=Component.translatable("gui.corpseorigin.disguise.error."+error);
    }
    @Override public void tick(){confirm.active=!waiting && (!other || !playerId.getValue().isBlank());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){
        if(waiting && Minecraft.getInstance().getConnection()!=null)ClientPlayNetworking.send(new ChameleonDisguisePayload.Select("#cancel"));
        super.removed();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);
        int y=Math.max(26,height/2-70);
        g.centeredText(font,title,width/2,y-20,0xffffffff);
        if(!other) {
            var texture=xiaoshi2022.corpseorigin.CorpseOrigin.id("textures/entity/xiaohuiskin.png");
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,width/2-16,y+29,8,8,32,32,8,8,64,64);
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,width/2-16,y+29,40,8,32,32,8,8,64,64);
        }
        g.centeredText(font,status,width/2,y+68,waiting?0xffcccccc:0xffff8888);
    }
}
