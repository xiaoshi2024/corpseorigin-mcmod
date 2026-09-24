package xiaoshi2022.corpseorigin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import xiaoshi2022.corpseorigin.growth.*;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;
import java.util.*;
import java.nio.file.Files;

/** Compact paged editor: one mounted organ at a time, eight slots per body. */
public final class OrganEditorScreen extends Screen {
    private final List<OrganSlot> slots=new ArrayList<>();
    private int selected,organIndex,jointIndex;
    private boolean mirror;
    private boolean previewPage;
    private float previewYaw=180, previewZoom=1;
    private int motionIndex;
    private final String[] motions={"idle","walk","attack","crouch","fly","glide","swim"};
    private final String[] motionNames={net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.001"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.002"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.003"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.004"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.005"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.006"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.007")};
    private int editorX(){return width>=620?width/2+5:width/2-150;}
    private final List<EditBox> values=new ArrayList<>();
    private final String[] labels={net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.008"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.009"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.010"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.011"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.012"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.013"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.014")};
    public OrganEditorScreen(){super(Component.translatable("message.corpseorigin.organ_editor_screen.text_01"));
        var p=Minecraft.getInstance().player;
        if(p!=null)try{slots.addAll(OrganLibrary.parseSlots(p.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr(OrganLibrary.BODY_KEY,"[]")));}catch(Exception ignored){}
    }
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        clearWidgets();values.clear();int x=editorX();
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.015"),4,4,90,()->{if(commit())minecraft.gui.setScreen(new OrganEvolutionScreen(this));});
        if(width<620)button(previewPage?net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.016"):net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.017"),width-90,4,85,()->{if(commit()){previewPage=!previewPage;init();}});
        if(width>=620 || previewPage){
            int px=width>=620?Math.max(8,width/2-305):width/2-150;
            button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.018"),px,height-51,55,()->previewYaw-=30);
            button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.019"),px+60,height-51,55,()->previewYaw+=30);
            button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.020"),px+120,height-51,55,()->previewZoom=Math.max(.25f,previewZoom-.15f));
            button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.021"),px+180,height-51,55,()->previewZoom=Math.min(2,previewZoom+.15f));
            button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.022")+motionNames[motionIndex],px,height-27,145,()->{if(commit()){motionIndex=(motionIndex+1)%motions.length;init();}});
            button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.023"),px+150,height-27,100,()->{previewYaw=180;previewZoom=1;});
        }
        if(width<620 && previewPage)return;
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.024"),width-145,width>=620?4:height-23,141,this::openExamples);
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.025"),width>=620?x:4,height-23,141,()->{if(commit())OrganClient.refreshPacks();});
        if(OrganClient.catalog.isEmpty()){button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.026"),width/2-50,height-28,100,this::onClose);return;}
        selected=Math.clamp(selected,0,Math.max(0,slots.size()-1));
        OrganSlot slot=slots.isEmpty()?new OrganSlot(OrganClient.catalog.get(0).id(),"body",0,0,0,0,0,0,1,false):slots.get(selected);
        organIndex=0;for(int i=0;i<OrganClient.catalog.size();i++)if(OrganClient.catalog.get(i).id().equals(slot.organ()))organIndex=i;
        jointIndex=OrganSlot.JOINTS.indexOf(slot.joint());mirror=slot.mirror();
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.027"),x,28,70,()->{if(commit()){selected=Math.max(0,selected-1);init();}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.028"),x+75,28,70,()->{if(commit()){selected=Math.min(slots.size()-1,selected+1);init();}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.029"),x+150,28,70,()->{if(slots.size()<8&&commit()){slots.add(new OrganSlot(OrganClient.catalog.get(0).id(),"body",0,0,0,0,0,0,1,false));selected=slots.size()-1;init();}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.030"),x+225,28,70,()->{if(!slots.isEmpty())slots.remove(selected);init();});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.031")+OrganClient.catalog.get(organIndex).displayName().getString(),x,53,295,()->{if(commit()){organIndex=(organIndex+1)%OrganClient.catalog.size();replaceChoice();init();}});
        String[] joints={net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.032"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.033"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.034"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.035"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.036"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.037"),net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.038")};
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.039")+joints[Math.max(0,jointIndex)],x,78,195,()->{if(commit()){jointIndex=(jointIndex+1)%joints.length;replaceChoice();init();}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.040")+(mirror?net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.041"):net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.042")),x+200,78,95,()->{if(commit()){mirror=!mirror;replaceChoice();init();}});
        float[] numbers={slot.x(),slot.y(),slot.z(),slot.rx(),slot.ry(),slot.rz(),slot.scale()};
        for(int i=0;i<7;i++){int col=i%3,row=i/3;var field=new EditBox(font,x+col*100,110+row*29,90,18,Component.literal(labels[i]));field.setMaxLength(8);field.setValue(Float.toString(numbers[i]));values.add(field);addRenderableWidget(field);}
        button(Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative() ? net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.043") : net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.044"),x,194,145,()->{if(commit()&&!OrganClient.pending){try{OrganLibrary.parseSlots(OrganLibrary.JSON.toJson(slots));}catch(Exception e){OrganClient.status=xiaoshi2022.corpseorigin.util.LocalizedException.describe(e);return;}OrganClient.pending=true;OrganClient.status=Component.translatable("gui.corpseorigin.label.045");ClientPlayNetworking.send(new OrganEditorPayload(OrganLibrary.JSON.toJson(slots)));}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.046"),x+150,194,145,()->{slots.clear();init();});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.047"),x,219,95,()->{if(commit())try{var f=preset();Files.createDirectories(f.getParent());Files.writeString(f,OrganLibrary.JSON.toJson(slots));OrganClient.status=Component.translatable("gui.corpseorigin.label.048");}catch(Exception e){OrganClient.status=Component.translatable("gui.corpseorigin.label.049");}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.050"),x+100,219,95,()->{try{var parsed=OrganLibrary.parseSlots(Files.readString(preset()));slots.clear();slots.addAll(parsed);init();OrganClient.status=Component.translatable("gui.corpseorigin.label.051");}catch(Exception e){OrganClient.status=Component.translatable("gui.corpseorigin.label.052");}});
        button(net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.026"),x+200,219,95,this::onClose);
    }
    private java.nio.file.Path preset(){return FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organ-preset.json");}
    public void refreshCatalog(){init();}
    private void openExamples(){
        try {
            var source=FabricLoader.getInstance().getModContainer("corpseorigin").orElseThrow()
                    .findPath("corpseorigin_examples/organ-pack").orElseThrow();
            var destination=FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organ/examples");
            Files.createDirectories(destination);
            try(var paths=Files.walk(source)){
                for(var entry:paths.toList()){
                    var target=destination.resolve(source.relativize(entry).toString());
                    if(Files.isDirectory(entry))Files.createDirectories(target);
                    else if(!Files.exists(target))Files.copy(entry,target);
                }
            }
            var root=destination.getParent();
            var bundled=destination.resolve("CorpseOrigin-Organs.zip");
            var pack=root.resolve("CorpseOrigin-Organs.zip");
            if(!Files.exists(pack))Files.copy(bundled,pack);
            net.minecraft.util.Util.getPlatform().openPath(root.toAbsolutePath());
            OrganClient.status=Component.translatable("gui.corpseorigin.label.053");
        }catch(Exception e){
            OrganClient.status=Component.translatable("gui.corpseorigin.label.054");
            xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot open organ examples",e);
            if(minecraft!=null && minecraft.player!=null)minecraft.player.sendSystemMessage(OrganClient.status);
        }
    }
    private void replaceChoice(){
        if(slots.isEmpty()){slots.add(new OrganSlot(OrganClient.catalog.get(organIndex).id(),OrganSlot.JOINTS.get(jointIndex),0,0,0,0,0,0,1,mirror));return;}
        var s=slots.get(selected);slots.set(selected,new OrganSlot(OrganClient.catalog.get(organIndex).id(),OrganSlot.JOINTS.get(jointIndex),s.x(),s.y(),s.z(),s.rx(),s.ry(),s.rz(),s.scale(),mirror));}
    private boolean commit(){
        if(values.size()!=7 || slots.isEmpty())return true;
        try{float[] n=new float[7];for(int i=0;i<7;i++)n[i]=Float.parseFloat(values.get(i).getValue());
            var s=new OrganSlot(OrganClient.catalog.get(organIndex).id(),OrganSlot.JOINTS.get(jointIndex),n[0],n[1],n[2],n[3],n[4],n[5],n[6],mirror);
            if(!s.valid())throw new IllegalArgumentException();slots.set(selected,s);return true;
        }catch(Exception e){OrganClient.status=Component.translatable("gui.corpseorigin.label.055");return false;}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractRenderState(g,mx,my,partial);g.centeredText(font,title,width/2,8,0xffffffff);
        if(OrganClient.catalog.isEmpty()){g.centeredText(font,Component.translatable("message.corpseorigin.organ_editor_screen.text_02"),width/2,50,0xffffffff);return;}
        if(width>=620 || !previewPage){
            int x=editorX();for(int i=0;i<7;i++)g.text(font,labels[i],x+(i%3)*100,100+(i/3)*29,0xffcccccc,false);
            g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.056")+slots.size()+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.057")+(slots.isEmpty()?0:selected+1),x+100,174,0xffffffff,false);
            g.text(font,font.plainSubstrByWidth(OrganClient.status.getString(),295),x,244,0xffffff55,false);
            if(mx>=x && mx<=x+295 && my>=244 && my<=257 && !OrganClient.status.equals(Component.empty()))
                g.setComponentTooltipForNextFrame(font,java.util.List.of(OrganClient.status),mx,my);
            g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.058"),x,257,0xffbbbbbb,false);
            g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.059"),x,270,0xffbbbbbb,false);
            if(organIndex<OrganClient.catalog.size()){
                var def=OrganClient.catalog.get(organIndex);
                if(def.waterJet()!=null && minecraft.player!=null){
                    int water=minecraft.player.getAttachedOrCreate(SurvivalGrowth.BODY).getIntOr("organ_water:"+def.id()+":"+def.waterJet().materialType(),0);
                    g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.060")+def.waterJet().materialType()+" "+water+" / "+def.waterJet().capacity()+net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.061"),x,283,0xff77ccff,false);
                    if(my>=270)g.setComponentTooltipForNextFrame(font,java.util.List.of(Component.translatable("message.corpseorigin.organ_editor_screen.text_03"),Component.translatable("message.corpseorigin.organ_editor_screen.text_04")),mx,my);
                }
            }
        }
        if(width>=620 || previewPage)renderPreview(g,partial);
    }
    private void renderPreview(GuiGraphicsExtractor g,float partial){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        commit();
        int left=width>=620?Math.max(8,width/2-305):width/2-150;
        int right=left+295,bottom=height-58;
        g.fill(left,28,right,bottom,0xff18212b);
        xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.beginPreview(slots,motions[motionIndex]);
        try{
            var state=mc.getEntityRenderDispatcher().extractEntity(mc.player,partial);
            if(state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar){
                avatar.bodyRot=180;avatar.yRot=0;avatar.xRot=0;avatar.scale=1;avatar.isInvisible=false;avatar.nameTag=null;
                avatar.walkAnimationSpeed=motionIndex==1?1:0;
                avatar.walkAnimationPos=avatar.ageInTicks*.6f;
                avatar.isCrouching=motionIndex==3;
            }
            var rotation=new org.joml.Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(previewYaw-180));
            g.entity(state,Math.max(12,(bottom-28)/2.5f)*previewZoom,new org.joml.Vector3f(0,1,0),rotation,new org.joml.Quaternionf(),left,28,right,bottom);
            g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.062"),left+5,32,0xffbbbbbb,false);
        }catch(Exception e){g.text(font,net.minecraft.client.resources.language.I18n.get("gui.corpseorigin.label.063"),left+5,50,0xffff7777,false);}
        finally{xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.endPreview();}
    }
    @Override public boolean isPauseScreen(){return false;}
}
