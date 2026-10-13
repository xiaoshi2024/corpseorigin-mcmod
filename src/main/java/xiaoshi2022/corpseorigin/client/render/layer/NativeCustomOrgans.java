package xiaoshi2022.corpseorigin.client.render.layer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.*;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.growth.*;
import java.util.*;

/** Independent animation state per host and slot; transforms remain in the original host's joint space. */
public final class NativeCustomOrgans {
    private static final DataTicket<String> CLIP = DataTicket.create("native_organ_clip", String.class);
    private static final DataTicket<List> FRAMES = DataTicket.create("native_organ_frames", List.class);
    private static EntityRendererProvider.Context context;
    private static Object resources, catalog;
    private static final Map<LivingEntity, Host> HOSTS = new WeakHashMap<>();
    private static final Set<String> BROKEN = new HashSet<>(), VALIDATED = new HashSet<>();
    private record Entry(OrganSlot slot, Renderer renderer) {}
    private record Host(String json, List<Entry> entries) {}
    private record Frame(Entry entry, LivingEntityRenderState state) {}
    public static void init(EntityRendererProvider.Context ctx) {
        if (context != ctx) { HOSTS.clear(); BROKEN.clear(); VALIDATED.clear(); }
        context = ctx;
    }
    private static boolean valid(OrganDefinition def) {
        if (BROKEN.contains(def.id())) return false;
        if (VALIDATED.contains(def.id())) return true;
        var manager = Minecraft.getInstance().getResourceManager();
        try (var geo = manager.getResource(Identifier.parse(def.model())).orElseThrow().openAsReader();
             var animation = manager.getResource(Identifier.parse(def.animation())).orElseThrow().openAsReader()) {
            manager.getResource(Identifier.parse(def.texture())).orElseThrow();
            var bones = com.google.gson.JsonParser.parseReader(geo).getAsJsonObject()
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            if (bones.size() > 128) throw new IllegalArgumentException("Maximum 128 bones");
            int cubes = 0;
            for (var bone : bones) {
                var boxes = bone.getAsJsonObject().getAsJsonArray("cubes");
                if (boxes != null) cubes += boxes.size();
            }
            if (cubes > 1024) throw new IllegalArgumentException("Maximum 1024 cubes");
            var clips = com.google.gson.JsonParser.parseReader(animation).getAsJsonObject().getAsJsonObject("animations");
            for (String clip : def.clips().values())
                if (!clips.has(clip)) throw new IllegalArgumentException("Missing clip: " + clip);
            VALIDATED.add(def.id());
            return true;
        } catch (Exception e) { broken(def.id(), e); return false; }
    }
    private static void broken(String id, Exception e) {
        if (BROKEN.add(id)) xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot render native organ {}: {}", id, e.getMessage());
    }
    public static void extract(LivingEntity entity, EntityRenderState parent, float partial) {
        parent.addGeckolibData(FRAMES, List.of());
        parent.addGeckolibData(NativeCorpseLayer.APPEARANCE, NativeCorpseState.appearance(entity));
        if (context == null || !NativeCorpseState.infected(entity) || entity.isInvisible()) return;
        var manager = Minecraft.getInstance().getResourceManager();
        if (resources != manager || catalog != OrganClient.catalog) {
            HOSTS.clear(); BROKEN.clear(); VALIDATED.clear(); resources = manager; catalog = OrganClient.catalog;
        }
        String json = NativeCorpseState.organLoadout(entity);
        Host host = HOSTS.get(entity);
        if (host == null || !host.json.equals(json)) {
            var entries = new ArrayList<Entry>();
            for (var slot : NativeCorpseState.organSlots(entity)) {
                if (slot.replacesBody()) continue;
                var def = OrganClient.catalog.stream().filter(d -> d.id().equals(slot.organ())).findFirst().orElse(null);
                if (def != null && valid(def)) entries.add(new Entry(slot, new Renderer(context, def)));
            }
            host = new Host(json, List.copyOf(entries)); HOSTS.put(entity, host);
        }
        var frames = new ArrayList<Frame>();
        for (Entry entry : host.entries) {
            if (BROKEN.contains(entry.slot.organ())) continue;
            try {
                String motion = entity.isInWater() ? "swim" : entity.swinging ? "attack"
                        : !entity.onGround() ? "fly" : entity.getDeltaMovement().horizontalDistanceSqr() > .0004 ? "walk" : "idle";
                var state = new LivingEntityRenderState();
                state.addGeckolibData(CLIP, entry.renderer.def.clips().getOrDefault(motion, entry.renderer.def.clips().get("idle")));
                entry.renderer.getAnimatable().host = new java.lang.ref.WeakReference<>(entity);
                entry.renderer.extractRenderState(entity, state, partial);
                frames.add(new Frame(entry, state));
            } catch (Exception e) { broken(entry.slot.organ(), e); }
        }
        parent.addGeckolibData(FRAMES, List.copyOf(frames));
    }
    public static void submit(EntityRenderState state, PoseStack poses, SubmitNodeCollector collector,
                              java.util.function.Function<String, List<ModelPart>> joints) {
        List<?> frames = state.getGeckolibData(FRAMES);
        if (frames == null) return;
        for (Object value : frames) {
            if (!(value instanceof Frame frame) || BROKEN.contains(frame.entry.slot.organ())) continue;
            var slot = frame.entry.slot;
            var path = joints.apply(slot.joint());
            if (path.isEmpty() || path.stream().anyMatch(p -> !p.visible)) continue;
            var local = new PoseStack(); local.last().set(poses.last());
            for (var part : path) part.translateAndRotate(local);
            local.translate(slot.x()/16.0, slot.y()/16.0, slot.z()/16.0);
            local.mulPose(Axis.XP.rotationDegrees(slot.rx()));
            local.mulPose(Axis.YP.rotationDegrees(slot.ry()));
            local.mulPose(Axis.ZP.rotationDegrees(slot.rz()));
            local.scale(slot.mirror() ? -slot.scale() : slot.scale(), -slot.scale(), -slot.scale());
            try { frame.entry.renderer.performRenderPass(frame.state, local, collector,
                    new net.minecraft.client.renderer.state.level.CameraRenderState()); }
            catch (Exception e) { broken(slot.organ(), e); }
        }
    }
    /** Molang entity queries must read the host, while animation caches belong to the independent organ. */
    public static Object queryHost(Object animatable) {
        if (animatable instanceof Adapter adapter && adapter.host != null) {
            LivingEntity entity = adapter.host.get();
            if (entity != null) return entity;
        }
        return animatable;
    }
    private static final class Adapter implements GeoAnimatable {
        private java.lang.ref.WeakReference<LivingEntity> host;
        private final AnimatableInstanceCache cache = new InstancedAnimatableInstanceCache(this);
        public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<Adapter>("organ", 4, test -> {
                String clip = test.getData(CLIP);
                return clip == null ? PlayState.STOP : test.setAndContinue(RawAnimation.begin().thenLoop(clip));
            }));
        }
    }
    private static final class Model extends DefaultedEntityGeoModel<Adapter> {
        final OrganDefinition def;
        Model(OrganDefinition def) { super(Identifier.parse("corpseorigin:organ_bat")); this.def = def; }
        @Override public Identifier getModelResource(GeoRenderState s) { return Identifier.parse(OrganResourceIds.model(def.model())); }
        @Override public Identifier getTextureResource(GeoRenderState s) { return Identifier.parse(def.texture()); }
        @Override public Identifier getAnimationResource(Adapter a) { return Identifier.parse(OrganResourceIds.animation(def.animation())); }
    }
    private static final class Renderer extends GeoReplacedEntityRenderer<Adapter, LivingEntity, LivingEntityRenderState> {
        final OrganDefinition def;
        Renderer(EntityRendererProvider.Context ctx, OrganDefinition def) { super(ctx, new Model(def), new Adapter()); this.def = def; shadowRadius = 0; }
        @Override public boolean shouldShowName(LivingEntity entity, double distance) { return false; }
        @Override public void adjustRenderPose(RenderPassInfo<LivingEntityRenderState> info) {}
        @Override public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float x, float y) {}
    }
}
