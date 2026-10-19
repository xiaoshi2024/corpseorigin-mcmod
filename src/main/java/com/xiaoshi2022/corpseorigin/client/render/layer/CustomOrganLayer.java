package com.xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.client.OrganClient;
import com.xiaoshi2022.corpseorigin.growth.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;

/** Separate animation cache for every entity and slot, with vanilla animated joint transforms. */
public final class CustomOrganLayer extends RenderLayer<LivingEntity, EntityModel<LivingEntity>> {
    private static final Map<LivingEntity, Host> HOSTS = new WeakHashMap<>();
    private static final Set<String> BROKEN = new HashSet<>(), VALIDATED = new HashSet<>();
    /** Shared per texture so every entity of the same organ batches into one render type / buffer. */
    private static final Map<String, RenderType> TYPES = new HashMap<>();
    /** Render-thread scratch stack; its matrices are overwritten from the parent pose on every organ. */
    private static PoseStack scratch = new PoseStack();
    private static boolean scratchDirty;
    private static List<OrganSlot> preview;
    private static LivingEntity previewHost;
    private static String previewMotion;
    private final NativeCorpseLayer joints;
    private record Entry(OrganSlot slot, Adapter adapter, Renderer renderer, RenderType type) {}
    private record Host(String loadout, List<OrganSlot> slots, int generation, List<Entry> entries, boolean replacesBody) {}
    public CustomOrganLayer(RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> parent) { super(parent); joints = new NativeCorpseLayer(parent); }
    public static void clear() { HOSTS.clear(); BROKEN.clear(); VALIDATED.clear(); TYPES.clear(); }
    public static void beginPreview(LivingEntity host, List<OrganSlot> slots, String motion) { previewHost=host; preview=slots; previewMotion=motion; }
    public static void endPreview() { if(previewHost!=null)HOSTS.remove(previewHost); previewHost=null; preview=null; }
    private static List<OrganSlot> slots(LivingEntity e) { return e==previewHost && preview!=null ? preview : NativeCorpseState.organSlots(e); }
    public static boolean replacesBody(LivingEntity e) { return host(e).replacesBody; }
    private static boolean validate(OrganDefinition d) {
        if (BROKEN.contains(d.id())) return false;
        if (VALIDATED.contains(d.id())) return true;
        var resources=Minecraft.getInstance().getResourceManager();
        try (var geo=resources.getResource(new ResourceLocation(d.model())).orElseThrow().openAsReader();
             var anim=resources.getResource(new ResourceLocation(d.animation())).orElseThrow().openAsReader()) {
            resources.getResource(new ResourceLocation(d.texture())).orElseThrow();
            var geometry=com.google.gson.JsonParser.parseReader(geo).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=geometry.getAsJsonArray("bones"); if (bones.size()>128) throw OrganText.error("bone_limit");
            int count=0; for(var bone:bones) { var cubes=bone.getAsJsonObject().getAsJsonArray("cubes"); if(cubes!=null)count+=cubes.size(); }
            if(count>1024)throw OrganText.error("cube_limit");
            var clips=com.google.gson.JsonParser.parseReader(anim).getAsJsonObject().getAsJsonObject("animations");
            for(String clip:d.clips().values())if(!clips.has(clip))throw OrganText.error("missing_clip",clip);
            // Bake before hiding the base body, so unsupported geometry fails open.
            new Model(d).getBakedModel(new ResourceLocation(d.model()));
            VALIDATED.add(d.id());return true;
        } catch(Exception ex) { broken(d.id(),ex);return false; }
    }
    private static void broken(String id, Exception ex) {
            if(BROKEN.add(id)) { OrganClient.status=OrganText.text("render_error",id,OrganText.failure(ex)); CorpseOrigin.LOGGER.warn("Cannot render custom organ {}",id,ex); }
    }
    /**
     * Cached per entity. The previous implementation parsed and re-serialised the organ JSON on every frame;
     * the loadout string is already the authoritative and normalised form, so comparing it is allocation-free.
     */
    private static Host host(LivingEntity entity) {
        Host cached=HOSTS.get(entity);
        if(entity==previewHost && preview!=null) {
            if(cached!=null && cached.loadout==null && cached.generation==OrganClient.generation && cached.slots.equals(preview)) return cached;
            return build(entity,null,preview);
        }
        String loadout=NativeCorpseState.organLoadout(entity);
        if(cached!=null && loadout.equals(cached.loadout) && cached.generation==OrganClient.generation) return cached;
        return build(entity,loadout,slots(entity));
    }
    private static Host build(LivingEntity entity, String loadout, List<OrganSlot> source) {
        List<OrganSlot> slots=List.copyOf(source);
        List<Entry> entries=new ArrayList<>(slots.size());
        boolean replacesBody=false;
        for(OrganSlot slot:slots) {
            OrganDefinition def=find(slot.organ());
            if(def==null || !validate(def)) continue;
            replacesBody|=slot.replacesBody();
            entries.add(new Entry(slot,new Adapter(def),new Renderer(new Model(def)),type(def.texture())));
        }
        Host host=new Host(loadout,slots,OrganClient.generation,List.copyOf(entries),replacesBody);
        HOSTS.put(entity,host);
        return host;
    }
    private static OrganDefinition find(String id) {
        for(int i=0;i<OrganClient.catalog.size();i++) { var d=OrganClient.catalog.get(i); if(d.id().equals(id)) return d; }
        return null;
    }
    private static RenderType type(String texture) {
        return TYPES.computeIfAbsent(texture, t -> RenderType.entityCutoutNoCull(new ResourceLocation(t)));
    }
    /** Reuse a single stack; a failed render may have left pushed poses behind, so replace it then. */
    private static PoseStack scratch() {
        if(scratchDirty) { scratch=new PoseStack(); scratchDirty=false; }
        return scratch;
    }
    @Override public void render(PoseStack poses, MultiBufferSource buffers, int light, LivingEntity entity,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if(entity.isInvisible())return;
        List<Entry> entries=host(entity).entries;
        for(int i=0;i<entries.size();i++) {
            Entry entry=entries.get(i);
            var s=entry.slot;if(BROKEN.contains(s.organ()))continue;
            // Keep malformed GEO exceptions from leaking a pushed pose into the host's renderer.
            var local=scratch();local.last().pose().set(poses.last().pose());local.last().normal().set(poses.last().normal());
            if(s.replacesBody())local.translate(0,1.5,0);
            else if(!joints.applyCustomJoint(getParentModel(),s.joint(),local))continue;
            local.translate(s.x()/16D,s.y()/16D,s.z()/16D);
            local.mulPose(Axis.XP.rotationDegrees(s.rx()));local.mulPose(Axis.YP.rotationDegrees(s.ry()));local.mulPose(Axis.ZP.rotationDegrees(s.rz()));
            local.scale(s.mirror()?-s.scale():s.scale(),-s.scale(),-s.scale());
            String motion=entity==previewHost?previewMotion:entity.isInWater()?"swim":entity.swinging?"attack":entity.isFallFlying()?"glide":!entity.onGround()?"fly":entity.isShiftKeyDown()?"crouch":amount>.02F?"walk":"idle";
            entry.adapter.clip=entry.adapter.def.clips().getOrDefault(motion,entry.adapter.def.clips().get("idle"));
            entry.adapter.tick=age;
            try { entry.renderer.render(local,entry.adapter,buffers,entry.type,buffers.getBuffer(entry.type),light);
            } catch(Exception ex) { scratchDirty=true; broken(s.organ(),ex); }
        }
    }
    private static final class Adapter implements GeoAnimatable {
        final OrganDefinition def; String clip; double tick;
        private String rawClip; private RawAnimation raw;
        final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        Adapter(OrganDefinition def){this.def=def;}
        public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        public double getTick(Object object){return tick;}
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
            controllers.add(new AnimationController<Adapter>(this,"organ",4,s->{
                if(clip==null)return PlayState.STOP;
                if(!clip.equals(rawClip)) { rawClip=clip; raw=RawAnimation.begin().thenLoop(clip); }
                return s.setAndContinue(raw);
            }));
        }
    }
    private static final class Model extends GeoModel<Adapter> {
        final OrganDefinition def;
        private final ResourceLocation model,texture,animation;
        Model(OrganDefinition def){this.def=def;this.model=new ResourceLocation(def.model());this.texture=new ResourceLocation(def.texture());this.animation=new ResourceLocation(def.animation());}
        public ResourceLocation getModelResource(Adapter a){return model;}
        public ResourceLocation getTextureResource(Adapter a){return texture;}
        public ResourceLocation getAnimationResource(Adapter a){return animation;}
    }
    private static final class Renderer extends GeoObjectRenderer<Adapter> {
        Renderer(Model model){super(model);}
        @Override public void preRender(PoseStack poses, Adapter a, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                boolean rerender, float partial, int light, int overlay, float r,float g,float b,float alpha) {
            // GeoObjectRenderer adds a block-centre offset; attached organs already have a joint origin.
            objectRenderTranslations.set(poses.last().pose());
        }
    }
}
