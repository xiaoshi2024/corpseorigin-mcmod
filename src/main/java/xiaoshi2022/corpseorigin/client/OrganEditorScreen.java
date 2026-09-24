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
    private final String[] motionNames={"待机","行走","攻击","蹲伏","飞行","滑翔","游泳"};
    private int editorX(){return width>=620?width/2+5:width/2-150;}
    private final List<EditBox> values=new ArrayList<>();
    private final String[] labels={"位移 X","位移 Y","位移 Z","旋转 X","旋转 Y","旋转 Z","缩放"};
    public OrganEditorScreen(){super(Component.literal("进化器官装配"));
        var p=Minecraft.getInstance().player;
        if(p!=null)try{slots.addAll(OrganLibrary.parseSlots(p.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr(OrganLibrary.BODY_KEY,"[]")));}catch(Exception ignored){}
    }
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        clearWidgets();values.clear();int x=editorX();
        button("进化天梯",4,4,90,()->{if(commit())minecraft.gui.setScreen(new OrganEvolutionScreen(this));});
        if(width<620)button(previewPage?"返回装配":"3D 预览",width-90,4,85,()->{if(commit()){previewPage=!previewPage;init();}});
        if(width>=620 || previewPage){
            int px=width>=620?Math.max(8,width/2-305):width/2-150;
            button("左转",px,height-51,55,()->previewYaw-=30);
            button("右转",px+60,height-51,55,()->previewYaw+=30);
            button("缩小",px+120,height-51,55,()->previewZoom=Math.max(.25f,previewZoom-.15f));
            button("放大",px+180,height-51,55,()->previewZoom=Math.min(2,previewZoom+.15f));
            button("动作："+motionNames[motionIndex],px,height-27,145,()->{if(commit()){motionIndex=(motionIndex+1)%motions.length;init();}});
            button("重置视角",px+150,height-27,100,()->{previewYaw=180;previewZoom=1;});
        }
        if(width<620 && previewPage)return;
        button("打开器官资源包目录",width-145,width>=620?4:height-23,141,this::openExamples);
        button("刷新并加载器官包",width>=620?x:4,height-23,141,()->{if(commit())OrganClient.refreshPacks();});
        if(OrganClient.catalog.isEmpty()){button("关闭",width/2-50,height-28,100,this::onClose);return;}
        selected=Math.clamp(selected,0,Math.max(0,slots.size()-1));
        OrganSlot slot=slots.isEmpty()?new OrganSlot(OrganClient.catalog.get(0).id(),"body",0,0,0,0,0,0,1,false):slots.get(selected);
        organIndex=0;for(int i=0;i<OrganClient.catalog.size();i++)if(OrganClient.catalog.get(i).id().equals(slot.organ()))organIndex=i;
        jointIndex=OrganSlot.JOINTS.indexOf(slot.joint());mirror=slot.mirror();
        button("上一个",x,28,70,()->{if(commit()){selected=Math.max(0,selected-1);init();}});
        button("下一个",x+75,28,70,()->{if(commit()){selected=Math.min(slots.size()-1,selected+1);init();}});
        button("添加",x+150,28,70,()->{if(slots.size()<8&&commit()){slots.add(new OrganSlot(OrganClient.catalog.get(0).id(),"body",0,0,0,0,0,0,1,false));selected=slots.size()-1;init();}});
        button("删除",x+225,28,70,()->{if(!slots.isEmpty())slots.remove(selected);init();});
        button("模型："+OrganClient.catalog.get(organIndex).name(),x,53,295,()->{if(commit()){organIndex=(organIndex+1)%OrganClient.catalog.size();replaceChoice();init();}});
        String[] joints={"躯干 / 背部","头","左肩 / 手臂","右肩 / 手臂","左腿","右腿","全身替换"};
        button("关节："+joints[Math.max(0,jointIndex)],x,78,195,()->{if(commit()){jointIndex=(jointIndex+1)%joints.length;replaceChoice();init();}});
        button("镜像："+(mirror?"开":"关"),x+200,78,95,()->{if(commit()){mirror=!mirror;replaceChoice();init();}});
        float[] numbers={slot.x(),slot.y(),slot.z(),slot.rx(),slot.ry(),slot.rz(),slot.scale()};
        for(int i=0;i<7;i++){int col=i%3,row=i/3;var field=new EditBox(font,x+col*100,110+row*29,90,18,Component.literal(labels[i]));field.setMaxLength(8);field.setValue(Float.toString(numbers[i]));values.add(field);addRenderableWidget(field);}
        button(Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative() ? "创造：直接保存" : "保存到身体",x,194,145,()->{if(commit()&&!OrganClient.pending){try{OrganLibrary.parseSlots(OrganLibrary.JSON.toJson(slots));}catch(Exception e){OrganClient.status=e.getMessage();return;}OrganClient.pending=true;OrganClient.status="等待服务器确认…";ClientPlayNetworking.send(new OrganEditorPayload(OrganLibrary.JSON.toJson(slots)));}});
        button("清空装配",x+150,194,145,()->{slots.clear();init();});
        button("导出预设",x,219,95,()->{if(commit())try{var f=preset();Files.createDirectories(f.getParent());Files.writeString(f,OrganLibrary.JSON.toJson(slots));OrganClient.status="预设已导出";}catch(Exception e){OrganClient.status="预设写入失败";}});
        button("导入预设",x+100,219,95,()->{try{var parsed=OrganLibrary.parseSlots(Files.readString(preset()));slots.clear();slots.addAll(parsed);init();OrganClient.status="已载入，点击保存到身体应用";}catch(Exception e){OrganClient.status="预设不存在或格式无效";}});
        button("关闭",x+200,219,95,this::onClose);
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
            OrganClient.status="目录：config/corpseorigin/organ；请在资源包菜单启用";
        }catch(Exception e){
            OrganClient.status="无法打开示例包，请查看游戏日志";
            xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot open organ examples",e);
            if(minecraft!=null && minecraft.player!=null)minecraft.player.sendSystemMessage(Component.literal(OrganClient.status));
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
        }catch(Exception e){OrganClient.status="位移 ±48px，旋转 ±180°，缩放 0.1–3";return false;}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractRenderState(g,mx,my,partial);g.centeredText(font,title,width/2,8,0xffffffff);
        if(OrganClient.catalog.isEmpty()){g.centeredText(font,Component.literal("服务器未提供器官目录"),width/2,50,0xffffffff);return;}
        if(width>=620 || !previewPage){
            int x=editorX();for(int i=0;i<7;i++)g.text(font,labels[i],x+(i%3)*100,100+(i/3)*29,0xffcccccc,false);
            g.text(font,"装配 "+slots.size()+" / 8 · 当前 "+(slots.isEmpty()?0:selected+1),x+100,174,0xffffffff,false);
            g.text(font,font.plainSubstrByWidth(OrganClient.status,295),x,244,0xffffff55,false);
            if(mx>=x && mx<=x+295 && my>=244 && my<=257 && !OrganClient.status.isEmpty())
                g.setComponentTooltipForNextFrame(font,java.util.List.of(Component.literal(OrganClient.status)),mx,my);
            g.text(font,"创造直接装配 · 生存需天梯I阶解锁",x,257,0xffbbbbbb,false);
            g.text(font,"左上角进化天梯：查看步骤 / 点数 / 加点",x,270,0xffbbbbbb,false);
            if(organIndex<OrganClient.catalog.size()){
                var def=OrganClient.catalog.get(organIndex);
                if(def.waterJet()!=null && minecraft.player!=null){
                    int water=minecraft.player.getAttachedOrCreate(SurvivalGrowth.BODY).getIntOr("organ_water:"+def.id()+":"+def.waterJet().materialType(),0);
                    g.text(font,"吞吐 · "+def.waterJet().materialType()+" "+water+" / "+def.waterJet().capacity()+" · 按键见控制设置",x,283,0xff77ccff,false);
                    if(my>=270)g.setComponentTooltipForNextFrame(font,java.util.List.of(Component.literal("器官特性键（默认G）：喷射；潜行+该键：吸水或吞入主手泥土/沙子。"),Component.literal("请先将该器官保存到身体。多个水枪器官使用装配顺序中的第一个。")),mx,my);
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
            g.text(font,"本地预览 · 保存后同步给其他玩家",left+5,32,0xffbbbbbb,false);
        }catch(Exception e){g.text(font,"预览失败，请检查资源和游戏日志",left+5,50,0xffff7777,false);}
        finally{xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.endPreview();}
    }
    @Override public boolean isPauseScreen(){return false;}
}
