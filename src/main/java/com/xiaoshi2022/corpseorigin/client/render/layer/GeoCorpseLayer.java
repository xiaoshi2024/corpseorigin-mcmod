package com.xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.client.render.NativeCorpseGeometry;
import com.xiaoshi2022.corpseorigin.client.render.NativeCorpseOrgans;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;
import java.util.*;

/** Attaches organs in the host's animated bone space, including every ancestor transform. */
@Mod.EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public final class GeoCorpseLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private record Anchor(GeoBone bone, NativeCorpseGeometry.Bounds bounds) {}
    private record Anchors(Anchor body, Anchor head) {}
    private final Map<BakedGeoModel, Anchors> anchors = new WeakHashMap<>();

    public GeoCorpseLayer(GeoRenderer<T> renderer) { super(renderer); }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register(GeoRenderEvent.Entity.CompileRenderLayers event) {
        event.addLayer(new GeoCorpseLayer(event.getRenderer()));
    }

    @Override
    public void renderForBone(PoseStack pose, T animatable, GeoBone bone, RenderType type,
            MultiBufferSource buffers, VertexConsumer buffer, float partial, int light, int overlay) {
        if (!(animatable instanceof LivingEntity entity) || entity.isInvisible() || bone.isHidden()
                || !NativeCorpseState.transformed(entity)) return;
        var player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(entity) > 48 * 48) return;
        Anchors selected = anchors.computeIfAbsent(getDefaultBakedModel(animatable), GeoCorpseLayer::select);
        int mask = NativeCorpseState.renderAppearance(entity);
        if ((mask & NativeCorpseState.RIBS) != 0 && selected.body != null && selected.body.bone == bone)
            draw(selected.body, false, pose, buffers, light);
        if ((mask & NativeCorpseState.EYE) != 0 && selected.head != null && selected.head.bone == bone)
            draw(selected.head, true, pose, buffers, light);
    }

    private static Anchors select(BakedGeoModel model) {
        List<Anchor> candidates = new ArrayList<>();
        for (GeoBone root : model.topLevelBones()) collect(root, candidates);
        Anchor largest = candidates.stream().max(Comparator.comparingDouble(a -> volume(a.bounds))).orElse(null);
        Anchor body = named(candidates, List.of("body", "torso", "chest", "upper_body", "main"), largest);
        return new Anchors(body, named(candidates, List.of("head", "head_main", "skull"), body));
    }

    private static Anchor named(List<Anchor> all, List<String> names, Anchor fallback) {
        for (String name : names) for (Anchor a : all)
            if (a.bone.getName().equalsIgnoreCase(name)) return a;
        for (String name : names) for (Anchor a : all) {
            String n = a.bone.getName().toLowerCase(Locale.ROOT);
            if (n.startsWith(name + "_") || n.endsWith("_" + name)) return a;
        }
        return fallback;
    }

    private static void collect(GeoBone bone, List<Anchor> out) {
        // Measure actual cube vertices (including inflation and cube rotation), in vanilla organ axes.
        float x=Float.POSITIVE_INFINITY,y=x,z=x,X=Float.NEGATIVE_INFINITY,Y=X,Z=X;
        for (GeoCube cube : bone.getCubes()) {
            PoseStack p = new PoseStack();
            RenderUtils.translateToPivotPoint(p, cube);
            RenderUtils.rotateMatrixAroundCube(p, cube);
            RenderUtils.translateAwayFromPivotPoint(p, cube);
            for (GeoQuad quad : cube.quads()) if (quad != null) for (GeoVertex vertex : quad.vertices()) {
                var v = p.last().pose().transformPosition(new org.joml.Vector3f(vertex.position()));
                float vx=v.x*16,vy=-v.y*16,vz=-v.z*16;
                x=Math.min(x,vx);X=Math.max(X,vx);y=Math.min(y,vy);Y=Math.max(Y,vy);z=Math.min(z,vz);Z=Math.max(Z,vz);
            }
        }
        if (Float.isFinite(x)) out.add(new Anchor(bone, new NativeCorpseGeometry.Bounds(x,y,z,X,Y,Z)));
        for (GeoBone child : bone.getChildBones()) collect(child, out);
    }

    private static double volume(NativeCorpseGeometry.Bounds b) {
        return (b.maxX()-b.minX())*(b.maxY()-b.minY())*(b.maxZ()-b.minZ());
    }

    private static void draw(Anchor anchor, boolean eye, PoseStack pose, MultiBufferSource buffers, int light) {
        var box = anchor.bounds;
        float width=Math.max(.5f,box.maxX()-box.minX()),height=Math.max(.5f,box.maxY()-box.minY());
        var organ = eye ? NativeCorpseOrgans.eye() : NativeCorpseOrgans.ribs();
        pose.pushPose();
        pose.scale(1,-1,-1);
        if (eye) {
            var placement=NativeCorpseGeometry.sproutingEye(box);
            pose.translate(placement.x()/16,placement.y()/16,placement.z()/16);
            pose.scale(placement.scale(),placement.scale(),placement.scale());
        } else {
            float depth=Math.min(width,height)/8;
            var bounds=NativeCorpseGeometry.bounds(organ);
            pose.translate((box.minX()+box.maxX())/32,(box.minY()+height*.5f)/16,
                    (box.minZ()-.05f-bounds.maxZ()*depth)/16);
            pose.scale(width/9,height/12,depth);
        }
        organ.render(pose,buffers.getBuffer(eye ? RenderType.entityTranslucent(NativeCorpseOrgans.EYE)
                : RenderType.entityCutoutNoCull(NativeCorpseOrgans.TISSUE)),light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
