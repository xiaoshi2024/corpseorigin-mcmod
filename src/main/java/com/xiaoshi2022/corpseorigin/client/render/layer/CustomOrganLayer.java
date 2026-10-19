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
    private static List<OrganSlot> preview;
    private static LivingEntity previewHost;
    private static String previewMotion;
    private final NativeCorpseLayer joints;
    private record Entry(OrganSlot slot, Adapter adapter, Renderer renderer) {}
    private record Host(String json, int generation, List<Entry> entries) {}
    public CustomOrganLayer(RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> parent) { super(parent); joints = new NativeCorpseLayer(parent); }
    public static void clear() { HOSTS.clear(); BROKEN.clear(); VALIDATED.clear(); }
    public static void beginPreview(LivingEntity host, List<OrganSlot> slots, String motion) { previewHost=host; preview=List.copyOf(slots); previewMotion=motion; }
    public static void endPreview() { previewHost=null; preview=null; }
    private static List<OrganSlot> slots(LivingEntity e) { return e==previewHost && preview!=null ? preview : NativeCorpseState.organSlots(e); }
    public static boolean replacesBody(LivingEntity e) {
        return slots(e).stream().anyMatch(s -> s.replacesBody() && OrganClient.catalog.stream().anyMatch(d -> d.id().equals(s.organ()) && validate(d)));
    }
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
    @Override public void render(PoseStack poses, MultiBufferSource buffers, int light, LivingEntity entity,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if(entity.isInvisible())return;
        String json=OrganLibrary.toJson(slots(entity));
        Host host=HOSTS.get(entity);
        if(host==null || !host.json.equals(json) || host.generation!=OrganClient.generation) {
            var entries=new ArrayList<Entry>();
            for(var slot:slots(entity)) {
                var def=OrganClient.catalog.stream().filter(d->d.id().equals(slot.organ())).findFirst().orElse(null);
                if(def!=null && validate(def)) { var a=new Adapter(def); entries.add(new Entry(slot,a,new Renderer(new Model(def)))); }
            }
            host=new Host(json,OrganClient.generation,List.copyOf(entries));HOSTS.put(entity,host);
        }
        for(var entry:host.entries) {
            var s=entry.slot;if(BROKEN.contains(s.organ()))continue;
            // Keep malformed GEO exceptions from leaking a pushed pose into the host's renderer.
            var local=new PoseStack();local.last().pose().set(poses.last().pose());local.last().normal().set(poses.last().normal());
            if(s.replacesBody())local.translate(0,1.5,0);
            else if(!joints.applyCustomJoint(getParentModel(),s.joint(),local))continue;
            local.translate(s.x()/16D,s.y()/16D,s.z()/16D);
            local.mulPose(Axis.XP.rotationDegrees(s.rx()));local.mulPose(Axis.YP.rotationDegrees(s.ry()));local.mulPose(Axis.ZP.rotationDegrees(s.rz()));
            local.scale(s.mirror()?-s.scale():s.scale(),-s.scale(),-s.scale());
            String motion=entity==previewHost?previewMotion:entity.isInWater()?"swim":entity.swinging?"attack":entity.isFallFlying()?"glide":!entity.onGround()?"fly":entity.isShiftKeyDown()?"crouch":amount>.02F?"walk":"idle";
            entry.adapter.clip=entry.adapter.def.clips().getOrDefault(motion,entry.adapter.def.clips().get("idle"));
            entry.adapter.tick=age;
            try { var type=RenderType.entityCutoutNoCull(new ResourceLocation(entry.adapter.def.texture()));
                entry.renderer.render(local,entry.adapter,buffers,type,buffers.getBuffer(type),light);
            } catch(Exception ex) { broken(s.organ(),ex); }
        }
    }
    private static final class Adapter implements GeoAnimatable {
        final OrganDefinition def; String clip; double tick;
        final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        Adapter(OrganDefinition def){this.def=def;}
        public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        public double getTick(Object object){return tick;}
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
            controllers.add(new AnimationController<Adapter>(this,"organ",4,s->clip==null?PlayState.STOP:s.setAndContinue(RawAnimation.begin().thenLoop(clip))));
        }
    }
    private static final class Model extends GeoModel<Adapter> {
        final OrganDefinition def; Model(OrganDefinition def){this.def=def;}
        public ResourceLocation getModelResource(Adapter a){return new ResourceLocation(def.model());}
        public ResourceLocation getTextureResource(Adapter a){return new ResourceLocation(def.texture());}
        public ResourceLocation getAnimationResource(Adapter a){return new ResourceLocation(def.animation());}
    }
    private static final class Renderer extends GeoObjectRenderer<Adapter> {
        Renderer(Model model){super(model);}
        @Override public void preRender(PoseStack poses, Adapter a, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                boolean rerender, float partial, int light, int overlay, float r,float g,float b,float alpha) {
            // GeoObjectRenderer adds a block-centre offset; attached organs already have a joint origin.
            objectRenderTranslations=new org.joml.Matrix4f(poses.last().pose());
        }
    }
}
