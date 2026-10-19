package com.xiaoshi2022.corpseorigin.client;

import com.xiaoshi2022.corpseorigin.growth.*;
import com.xiaoshi2022.corpseorigin.network.OrganNetwork;
import com.xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.entity.LivingEntity;
import java.nio.file.*;
import java.util.*;

/** Drafts stay local until the server accepts Save. Offsets use model pixels. */
public final class OrganEditorScreen extends Screen {
    private final List<OrganSlot> slots = new ArrayList<>();
    private final List<EditBox> numbers = new ArrayList<>();
    private EditBox search;
    private int selected, target = -1, panelWidth, left;
    private LivingEntity host;
    private boolean preview, dirty, awaiting = true;
    private String filter = "", motion = "idle";
    private int zoom = 55;
    private float rotation;
    private String jointName(String joint) { return OrganText.label(joint); }
    public OrganEditorScreen() { super(OrganText.text("title")); }
    @Override protected void init() {
        if (host == null) host = minecraft.player;
        panelWidth = Math.min(330, width - 12); left = width >= 580 ? 8 : (width-panelWidth)/2;
        numbers.clear();
        button(left, 23, panelWidth/2-2, OrganText.label("target", target<0?OrganText.label("self"):host.getName().getString()), () -> selectTarget());
        button(left+panelWidth/2+2, 23, panelWidth/2-2, preview?OrganText.label("editor"):OrganText.label("tools"), () -> { if(commit()){preview=!preview;rebuildWidgets();} });
        if (preview) { tools(); return; }
        int w=(panelWidth-7*2)/8;
        for(int i=0;i<8;i++){ final int index=i; button(left+i*(w+2),45,w,(i==selected?"[":"")+(i+1)+(i==selected?"]":""),()->{if(commit()){selected=index;rebuildWidgets();}}); }
        search = new EditBox(font,left,67,panelWidth-82,18,OrganText.text("search"));search.setMaxLength(96);search.setValue(filter);
        search.setSuggestion(filter.isEmpty()?OrganText.label("search"):"");search.setResponder(s->{filter=s;search.setSuggestion(s.isEmpty()?OrganText.label("search"):"");});addRenderableWidget(search);
        button(left+panelWidth-80,67,39,"◀",()->cycleOrgan(-1));button(left+panelWidth-39,67,39,"▶",()->cycleOrgan(1));
        OrganSlot slot=current();
        var organButton=button(left,89,panelWidth/2-2,slot==null?OrganText.label("choose"):organName(slot.organ()),()->cycleOrgan(1));
        if(slot!=null)OrganClient.catalog.stream().filter(d->d.id().equals(slot.organ())).findFirst()
                .ifPresent(d->organButton.setTooltip(Tooltip.create(OrganText.text("ability."+d.trait()))));
        button(left+panelWidth/2+2,89,panelWidth/2-2,slot==null?OrganText.label("joint",jointName("body")):OrganText.label("joint",jointName(slot.joint())),()->{
            if(commit()&&current()!=null){var s=current();replace(new OrganSlot(s.organ(),OrganSlot.JOINTS.get((OrganSlot.JOINTS.indexOf(s.joint())+1)%7),s.x(),s.y(),s.z(),s.rx(),s.ry(),s.rz(),s.scale(),s.mirror()));rebuildWidgets();}
        });
        float[] values=slot==null?new float[]{0,0,0,0,0,0,1}:new float[]{slot.x(),slot.y(),slot.z(),slot.rx(),slot.ry(),slot.rz(),slot.scale()};
        for(int i=0;i<7;i++){int row=i/3,col=i%3;EditBox box=new EditBox(font,left+col*(panelWidth/3)+22,112+row*22,panelWidth/3-25,18,Component.literal(OrganText.label("number")));box.setMaxLength(9);box.setValue(Float.toString(values[i]));box.setResponder(s->dirty=true);numbers.add(box);addRenderableWidget(box);}
        button(left+panelWidth/3,156,panelWidth/3-2,slot!=null&&slot.mirror()?OrganText.label("mirror_on"):OrganText.label("mirror_off"),()->{if(commit()&&current()!=null){var s=current();replace(new OrganSlot(s.organ(),s.joint(),s.x(),s.y(),s.z(),s.rx(),s.ry(),s.rz(),s.scale(),!s.mirror()));rebuildWidgets();}});
        button(left+panelWidth*2/3,156,panelWidth/3, OrganText.label("delete"),()->{if(selected<slots.size()){slots.remove(selected);dirty=true;rebuildWidgets();}});
        actions(180);
    }
    private void actions(int y){
        button(left,y,panelWidth/3-2,OrganText.label("save"),()->{if(commit()){OrganClient.pending=true;OrganNetwork.request("save",target,OrganLibrary.toJson(slots));}});
        button(left+panelWidth/3,y,panelWidth/3-2,OrganText.label("summon"),()->{if(commit())OrganNetwork.request("summon",target,OrganLibrary.toJson(slots));});
        button(left+panelWidth*2/3,y,panelWidth/3,OrganText.label("done"),this::onClose);
    }
    private void tools(){
        int y=Math.max(105,height-110);
        button(left,y,panelWidth/2-2,OrganText.label("folder"),()->{try{Files.createDirectories(OrganLibrary.directory());Util.getPlatform().openFile(OrganLibrary.directory().toFile());}catch(Exception e){error(e);}});
        button(left+panelWidth/2+2,y,panelWidth/2-2,OrganText.label("reload"),()->OrganClient.refreshPacks(target));
        button(left,y+22,panelWidth/2-2,OrganText.label("save_preset"),()->{try{Files.createDirectories(OrganLibrary.directory());Files.writeString(preset(),OrganLibrary.toJson(slots));OrganClient.status=OrganText.text("preset_saved");}catch(Exception e){error(e);}});
        button(left+panelWidth/2+2,y+22,panelWidth/2-2,OrganText.label("load_preset"),()->{try{var loaded=OrganLibrary.parseSlots(Files.readString(preset()));slots.clear();slots.addAll(loaded);dirty=true;OrganClient.status=OrganText.text("preset_loaded");}catch(Exception e){error(e);}});
        button(left,y+44,panelWidth/3-2,OrganText.label("smaller"),()->zoom=Math.max(15,zoom-10));
        button(left+panelWidth/3,y+44,panelWidth/3-2,OrganText.label("motion",OrganText.label(motion)),()->{var modes=List.of("idle","walk","attack","crouch","fly","glide","swim");motion=modes.get((modes.indexOf(motion)+1)%modes.size());rebuildWidgets();});
        button(left+panelWidth*2/3,y+44,panelWidth/3,OrganText.label("larger"),()->zoom=Math.min(100,zoom+10));actions(y+66);
    }
    private Path preset(){return OrganLibrary.directory().resolveSibling("organ-preset.json");}
    private Button button(int x,int y,int w,String label,Runnable click){return addRenderableWidget(Button.builder(Component.literal(label),b->click.run()).bounds(x,y,w,18).build());}
    private OrganSlot current(){return selected<slots.size()?slots.get(selected):null;}
    private String organName(String id){return OrganClient.catalog.stream().filter(d->d.id().equals(id)).findFirst().map(d->d.displayName().getString()).orElse(id);}
    private void replace(OrganSlot s){if(selected>=slots.size()){selected=slots.size();slots.add(s);}else slots.set(selected,s);dirty=true;}
    private boolean commit(){
        if(preview||numbers.size()!=7||current()==null)return true;
        try{float[] v=new float[7];for(int i=0;i<7;i++)v[i]=Float.parseFloat(numbers.get(i).getValue());var s=current();var n=new OrganSlot(s.organ(),s.joint(),v[0],v[1],v[2],v[3],v[4],v[5],v[6],s.mirror());if(!n.valid())throw OrganText.error("range");if(!n.equals(s))replace(n);return true;}catch(Exception e){OrganClient.status=OrganText.text("range");return false;}
    }
    private void cycleOrgan(int direction){
        if(!commit())return;
        var defs=OrganClient.catalog.stream().filter(d->d.id().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))||d.displayName().getString().contains(filter)).toList();
        if(defs.isEmpty()){OrganClient.status=OrganText.text("no_match");return;}
        var s=current();int index=-1;for(int i=0;i<defs.size();i++)if(s!=null&&defs.get(i).id().equals(s.organ()))index=i;
        String id=defs.get(Math.floorMod(index+direction,defs.size())).id();
        replace(s==null?new OrganSlot(id,"body",0,0,0,0,0,0,1,false):new OrganSlot(id,s.joint(),s.x(),s.y(),s.z(),s.rx(),s.ry(),s.rz(),s.scale(),s.mirror()));rebuildWidgets();
    }
    private void selectTarget(){
        if(!commit())return;
        if(target<0&&minecraft.crosshairPickEntity instanceof LivingEntity living&&NativeCorpseState.infected(living)){host=living;target=living.getId();}
        else{host=minecraft.player;target=-1;}
        slots.clear();slots.addAll(NativeCorpseState.organSlots(host));selected=0;dirty=false;awaiting=true;OrganNetwork.request("catalog",target,"[]");rebuildWidgets();
    }
    public void refreshCatalog(){if(!dirty)rebuildWidgets();}
    public void receiveState(UUID id){if(host!=null&&host.getUUID().equals(id)&&awaiting&&!dirty){slots.clear();slots.addAll(NativeCorpseState.organSlots(host));awaiting=false;rebuildWidgets();}}
    @Override public void tick(){if(awaiting&&host!=null&&!dirty){slots.clear();slots.addAll(NativeCorpseState.organSlots(host));}if(dirty&&!preview)commit();for(var n:numbers)n.tick();if(search!=null&&!preview)search.tick();}
    private void error(Exception e){OrganClient.status=OrganText.failure(e);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.drawCenteredString(font,title,width/2,8,0xFFFFFF);
        if(!preview){String[] labels={"X","Y","Z","RX","RY","RZ",OrganText.label("scale")};for(int i=0;i<7;i++)g.drawString(font,labels[i],left+(i%3)*(panelWidth/3),117+(i/3)*22,0xDDDDDD,false);}
        super.render(g,mx,my,partial);
        if(host!=null&&(preview||width>=580)){
            int x=preview?left+panelWidth/2:(width+left+panelWidth)/2;
            int bottom=preview?Math.max(95,height-115):height-45;
            int size=preview?Math.min(zoom,Math.max(18,(bottom-48)/2)):zoom;
            CustomOrganLayer.beginPreview(host,slots,motion);
            try{InventoryScreen.renderEntityInInventoryFollowsMouse(g,x,bottom,size,(float)(x-mx)+rotation,(float)(bottom-35-my),host);}catch(Exception e){error(e);}finally{CustomOrganLayer.endPreview();}
        }
        var lines=font.split(OrganClient.status,width-16);int y=height-12-Math.min(2,lines.size()-1)*10;
        for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(font,lines.get(i),8,y+i*10,0xFFCC77);
    }
}
