package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.growth.*;
import java.util.*;

/** Apply the vanilla animated joint pose, then render local-space GEO bones and their own animation. */
public final class CustomOrganLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    public static final DataTicket<String> CLIP = DataTicket.create("custom_organ_clip",String.class);
    public static final DataTicket<List> SNAPSHOTS = DataTicket.create("custom_organ_snapshots",List.class);
    private static EntityRendererProvider.Context context;
    private static final Map<String,Renderer> RENDERERS = new HashMap<>();
    private static final java.util.Set<String> BROKEN = new java.util.HashSet<>();
    private static final java.util.Set<String> VALIDATED = new java.util.HashSet<>();
    private static net.minecraft.server.packs.resources.ResourceManager resourceManager;
    private static List<OrganSlot> previewSlots;
    private static String previewMotion = "idle";
    public static void beginPreview(List<OrganSlot> slots, String motion) { previewSlots=List.copyOf(slots);previewMotion=motion; }
    public static void endPreview() { previewSlots=null; }
    private record Frame(OrganSlot slot, Renderer renderer, AvatarRenderState state) {}
    @SuppressWarnings({"rawtypes","unchecked"})
    public CustomOrganLayer(RenderLayerParent parent, EntityRendererProvider.Context ctx) {super(parent);if(context!=ctx)clearCatalog();context=ctx;}
    public static void clearCatalog(){RENDERERS.clear();BROKEN.clear();VALIDATED.clear();}
    private static boolean validate(OrganDefinition def,net.minecraft.server.packs.resources.ResourceManager resources){
        if(resourceManager!=resources){clearCatalog();resourceManager=resources;}
        if(BROKEN.contains(def.id()))return false;
        if(VALIDATED.contains(def.id()))return true;
        try(var model=resources.getResource(Identifier.parse(def.model())).orElseThrow().openAsReader();
            var animation=resources.getResource(Identifier.parse(def.animation())).orElseThrow().openAsReader()){
            var geo=com.google.gson.JsonParser.parseReader(model).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            if(geo.getAsJsonArray("bones").size()>128)throw new IllegalArgumentException("Maximum 128 bones");
            int cubes=0;for(var b:geo.getAsJsonArray("bones")){var c=b.getAsJsonObject().getAsJsonArray("cubes");if(c!=null)cubes+=c.size();}
            if(cubes>1024)throw new IllegalArgumentException("Maximum 1024 cubes");
            var clips=com.google.gson.JsonParser.parseReader(animation).getAsJsonObject().getAsJsonObject("animations");
            for(String clip:def.clips().values())if(!clips.has(clip))throw new IllegalArgumentException("Missing clip: "+clip);
            VALIDATED.add(def.id());return true;
        }catch(Exception e){BROKEN.add(def.id());xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Invalid organ {}: {}",def.id(),e.getMessage());return false;}
    }
    public static void extract(AbstractClientPlayer player,AvatarRenderState parent,float partial) {
        parent.addGeckolibData(SNAPSHOTS,List.of());
        var corpse=xiaoshi2022.corpseorigin.client.CorpseOriginClient.corpseDataCache.get(player.getUUID());
        boolean preview=previewSlots!=null && player==Minecraft.getInstance().player;
        // Saved loadouts have already passed server-side creative/evolution/level validation.
        if(context==null || !preview&&(parent.isInvisible || corpse!=null && corpse.isDisguised()))return;
        var body=player.getAttachedOrCreate(SurvivalGrowth.BODY);
        String saved=body.getStringOr(OrganLibrary.BODY_KEY, "");
        var frames=new ArrayList<Frame>();
        if(!preview && GourdOrganState.active(player) && !GourdOrganState.detached(player) && !GourdOrganState.dead(player)) {
            int form=GourdOrganState.form(player);
            String color=GourdOrganState.color(form);
            var def=new OrganDefinition("builtin_gourd_"+color,"organ.corpseorigin.gourd","cosmetic",
                    "corpseorigin:geckolib/models/entity/zbr_gourd.geo.json",
                    "corpseorigin:textures/entity/zbr_gourd/"+color+".png",
                    "corpseorigin:geckolib/animations/entity/zbr_gourd.animation.json",Map.of("idle",GourdOrganState.clip(form,false)));
            // Reserve a separate animation instance from the eight equipped and eight preview slots.
            String key=def.id()+":16";
            Renderer r=RENDERERS.computeIfAbsent(key,k->new Renderer(context,def,16));
            if(((PlayerGeoAnimatable)player).getAnimatableInstanceCache() instanceof xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache cache)
                cache.prepareOrgan(Long.MIN_VALUE+17,r);
            var state=new AvatarRenderState();state.addGeckolibData(CLIP,GourdOrganState.clip(form,false));
            r.extractRenderState(player,state,partial);
            state.addGeckolibData(xiaoshi2022.corpseorigin.client.render.GourdMouthAnchors.ENTITY,player.getId());
            frames.add(new Frame(new OrganSlot(def.id(),"body",0,14,5,0,0,0,.45f,false),r,state));
        }
        try {
            var slots=preview?previewSlots:OrganLibrary.parseSlots(saved.isEmpty()?"[]":saved);
            for(int i=0;i<slots.size();i++) {
                var slot=slots.get(i);var def=OrganClient.catalog.stream().filter(d->d.id().equals(slot.organ())).findFirst().orElse(null);
                if(def==null)continue;
                if(!preview && !player.isCreative() && OrganEvolution.stage(player,def.id())<=0)continue;
                var resources=Minecraft.getInstance().getResourceManager();
                if(resources.getResource(Identifier.parse(def.model())).isEmpty() || resources.getResource(Identifier.parse(def.texture())).isEmpty()
                        || resources.getResource(Identifier.parse(def.animation())).isEmpty())continue;
                if(!validate(def,resources))continue;
                int cacheSlot=i+(preview?8:0);
                String key=def.id()+":"+cacheSlot;
                if(BROKEN.contains(def.id()))continue;
                Renderer r=RENDERERS.get(key);
                if(r==null){r=new Renderer(context,def,cacheSlot);RENDERERS.put(key,r);}
                if (((PlayerGeoAnimatable)player).getAnimatableInstanceCache() instanceof xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache cache)
                    cache.prepareOrgan(Long.MIN_VALUE+1+cacheSlot,r);
                var state=new AvatarRenderState();
                String motion=player.isInWater()?"swim":player.swinging?"attack":!player.onGround()?(player.isShiftKeyDown()?"glide":"fly")
                        :player.isShiftKeyDown()?"crouch":parent.walkAnimationSpeed>.02?"walk":"idle";
                state.addGeckolibData(CLIP,def.clips().getOrDefault(preview?previewMotion:motion,def.clips().get("idle")));
                r.extractRenderState(player,state,partial);
                frames.add(new Frame(slot,r,state));
            }
        }catch(Exception e){/* Missing or obsolete presets fall back without changing saved data. */}
        parent.addGeckolibData(SNAPSHOTS,List.copyOf(frames));
    }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch) {
        submitFrames(getParentModel(),poses,collector,state);
    }
    public static void submitFrames(PlayerModel model,PoseStack poses,SubmitNodeCollector collector,AvatarRenderState state) {
        List<?> frames=state.getGeckolibData(SNAPSHOTS);if(frames==null || state.isInvisible)return;
        for(Object o:frames){if(!(o instanceof Frame frame))continue;
            var s=frame.slot;
            var joint=switch(s.joint()){case "head"->model.head;case "left_arm"->model.leftArm;case "right_arm"->model.rightArm;
                case "left_leg"->model.leftLeg;case "right_leg"->model.rightLeg;default->model.body;};
            // Isolate malformed custom models from the player's shared pose stack.
            var local=new PoseStack();local.last().set(poses.last());
            if(s.replacesBody())local.translate(0,1.5,0);else joint.translateAndRotate(local);
            local.translate(s.x()/16.0,s.y()/16.0,s.z()/16.0);
            local.mulPose(Axis.XP.rotationDegrees(s.rx()));local.mulPose(Axis.YP.rotationDegrees(s.ry()));local.mulPose(Axis.ZP.rotationDegrees(s.rz()));
            local.scale(s.mirror()?-s.scale():s.scale(),-s.scale(),-s.scale());
            try{frame.renderer.performRenderPass(frame.state,local,collector,new net.minecraft.client.renderer.state.level.CameraRenderState());}
            catch(Exception e){if(BROKEN.add(frame.renderer.def.id()))xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot render organ {}",frame.renderer.def.id(),e);}
        }
    }
    public static boolean hasCustomFrames(AvatarRenderState state){
        List<?> frames=state.getGeckolibData(SNAPSHOTS);return frames!=null&&!frames.isEmpty();
    }
    public static boolean replacesBody(AvatarRenderState state){
        List<?> frames=state.getGeckolibData(SNAPSHOTS);
        return frames!=null && frames.stream().anyMatch(o->o instanceof Frame f && f.slot.replacesBody() && !BROKEN.contains(f.renderer.def.id()));
    }
    private static final class Model extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {
        final OrganDefinition def;
        Model(OrganDefinition def){super(Identifier.parse("corpseorigin:organ_bat"));this.def=def;}
        @Override public Identifier getModelResource(GeoRenderState s){return Identifier.parse(OrganResourceIds.model(def.model()));}
        @Override public Identifier getTextureResource(GeoRenderState s){return Identifier.parse(def.texture());}
        @Override public Identifier getAnimationResource(PlayerGeoAnimatable a){return Identifier.parse(OrganResourceIds.animation(def.animation()));}
    }
    private static final class Renderer extends GeoReplacedEntityRenderer<PlayerGeoAnimatable,AbstractClientPlayer,AvatarRenderState> {
        final OrganDefinition def; final int slot;
        Renderer(EntityRendererProvider.Context ctx,OrganDefinition def,int slot){super(ctx,new Model(def),null);this.def=def;this.slot=slot;shadowRadius=0;}
        @Override public long getInstanceId(PlayerGeoAnimatable a,AbstractClientPlayer e){return Long.MIN_VALUE+1+slot;}
        @Override public AvatarRenderState fillRenderState(PlayerGeoAnimatable a,AbstractClientPlayer e,AvatarRenderState s,float p){return super.fillRenderState((PlayerGeoAnimatable)e,e,s,p);}
        @Override public AvatarRenderState createRenderState(PlayerGeoAnimatable a,AbstractClientPlayer e){return super.createRenderState((PlayerGeoAnimatable)e,e);}
        @Override public Identifier getTextureLocation(AvatarRenderState s){return Identifier.parse(def.texture());}
        @Override public void adjustRenderPose(RenderPassInfo<AvatarRenderState> info){}
        @Override public void preRenderPass(RenderPassInfo<AvatarRenderState> info,SubmitNodeCollector collector){super.preRenderPass(info,collector);if(slot==16)xiaoshi2022.corpseorigin.client.render.GourdMouthAnchors.listen(info,true);}
        @Override public void scaleModelForRender(RenderPassInfo<AvatarRenderState> info,float x,float y){}
    }
}
