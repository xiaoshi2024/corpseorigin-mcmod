package xiaoshi2022.corpseorigin.client.render.layer;

import com.geckolib.constant.dataticket.DataTicket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.mixin.NativeCorpseModelPartAccessor;

import java.util.*;

/** Additive surfaces and organs in the animated host's joint space. Never hides or replaces its model. */
public final class NativeCorpseLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {
    public static final DataTicket<Boolean> INFECTED = DataTicket.create("native_corpse_infected", Boolean.class);
    public static final DataTicket<Integer> APPEARANCE = DataTicket.create("native_corpse_appearance", Integer.class);
    private final Organs organs = new Organs();

    public NativeCorpseLayer(RenderLayerParent<S, M> parent) { super(parent); }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, S state, float yaw, float pitch) {
        if (state.isInvisible || !Boolean.TRUE.equals(state.getGeckolibData(INFECTED))) return;
        organs.submit(getParentModel(), state, poses, collector, light);
    }

    /** Also shared by the dragon's separate renderer. Cache paths, never entity poses or render states. */
    public static final class Organs {
        private static final Identifier VEINS = CorpseOrigin.id("textures/entity/native_corpse_veins.png");
        private static final Identifier TISSUE = CorpseOrigin.id("textures/entity/native_corpse_organs.png");
        private final Map<ModelPart, Anchors> anchors = new IdentityHashMap<>();
        private final ModelPart ribs;
        private final ModelPart eye;

        public Organs() {
            var ribCubes = CubeListBuilder.create().texOffs(0, 16).addBox(-3.3f, -4.5f, -.5f, 6.6f, 9, .8f)
                    .texOffs(0, 0).addBox(-.4f, -4.5f, -1.3f, .8f, 9, .8f);
            for (int i = 0; i < 4; i++) {
                float y = -3.8f + i * 2.2f;
                ribCubes.texOffs(0, 0).addBox(-3.7f, y, -1.2f, 3.4f, .65f, .8f)
                        .addBox(.3f, y, -1.2f, 3.4f, .65f, .8f)
                        .addBox(-4, y, -1, .6f, 1.3f, 1.5f)
                        .addBox(3.4f, y, -1, .6f, 1.3f, 1.5f);
            }
            ribs = bake(ribCubes);
            eye = new xiaoshi2022.corpseorigin.client.model.ExoskeletonModel(
                    xiaoshi2022.corpseorigin.client.model.ExoskeletonModel.createBodyLayer().bakeRoot()).getShieye();
            eye.x = eye.y = eye.z = 0;
        }

        private static ModelPart bake(CubeListBuilder cubes) {
            var mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("organ", cubes, PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("organ");
        }

        public <S extends EntityRenderState> void submit(Model<? super S> model, S state, PoseStack poses,
                                                        SubmitNodeCollector collector, int light) {
            veins(model.root(), "root", poses, collector, light, state.outlineColor);
            Anchors found = anchors.computeIfAbsent(model.root(), Organs::findAnchors);
            Integer flags = state.getGeckolibData(APPEARANCE);
            int mask = flags == null ? 0 : flags;
            if ((mask & 2) != 0) attach(found.body(), ribs, false, poses, collector, light, state.outlineColor);
            if ((mask & 1) != 0) attach(found.head(), eye, true, poses, collector, light, state.outlineColor);
            NativeCustomOrgans.submit(state, poses, collector, joint -> {
                var parts = new ArrayList<Anchor>();
                collect(model.root(), "root", new ArrayList<>(), parts);
                var names = switch (joint) {
                    case "head" -> List.of("head", "head_main", "center_head");
                    case "left_arm" -> List.of("left_arm", "left_front_leg", "left_front", "left_wing");
                    case "right_arm" -> List.of("right_arm", "right_front_leg", "right_front", "right_wing");
                    case "left_leg" -> List.of("left_leg", "left_hind_leg", "left_hind");
                    case "right_leg" -> List.of("right_leg", "right_hind_leg", "right_hind");
                    default -> List.of("body", "torso", "chest");
                };
                Anchor anchor = named(parts, names, found.body());
                return anchor == null ? List.of() : anchor.path();
            });
        }

        private final Map<ModelPart, ModelPart> surfaces = new IdentityHashMap<>();
        private void veins(ModelPart part, String name, PoseStack poses, SubmitNodeCollector collector, int light, int outline) {
            if (!part.visible || name.equals("hat") || name.equals("hat_rim") || name.equals("brim")) return;
            poses.pushPose();
            part.translateAndRotate(poses);
            var access = (NativeCorpseModelPartAccessor)(Object)part;
            if (!part.skipDraw && !access.corpseorigin$cubes().isEmpty()) {
                ModelPart surface = surfaces.computeIfAbsent(part, p -> new ModelPart(access.corpseorigin$cubes(), Map.of()));
                collector.order(1).submitModelPart(surface, poses, RenderTypes.entityCutoutZOffset(VEINS),
                        light, OverlayTexture.NO_OVERLAY, null, -1, null, outline);
            }
            access.corpseorigin$children().forEach((childName, child) -> veins(child, childName, poses, collector, light, outline));
            poses.popPose();
        }
        private static void attach(Anchor anchor, ModelPart organ, boolean eye, PoseStack poses,
                                   SubmitNodeCollector collector, int light, int outline) {
            if (anchor == null || anchor.path().stream().anyMatch(part -> !part.visible)) return;
            ModelPart joint = anchor.path().getLast();
            if (joint.skipDraw) return;
            var box = anchor.cube();
            poses.pushPose();
            // Include every ancestor: quadrupeds, nested heads, babies and fish all keep their own pose.
            for (ModelPart part : anchor.path()) part.translateAndRotate(poses);
            float width = Math.max(.5f, box.maxX - box.minX);
            float height = Math.max(.5f, box.maxY - box.minY);
            poses.translate((box.minX + box.maxX) / 32f,
                    (box.minY + height * (eye ? .32f : .5f)) / 16f, (box.minZ - .15f) / 16f);
            if (eye) {
                float scale = Math.max(.15f, Math.min(width, height) / 7f);
                poses.scale(scale, scale, scale);
            } else {
                poses.scale(width / 9f, height / 12f, Math.min(width, height) / 8f);
            }
            collector.order(2).submitModelPart(organ, poses, eye ? RenderTypes.entityTranslucent(CorpseOrigin.id("textures/entity/lower_level_zb_eye.png")) : RenderTypes.entityCutout(TISSUE),
                    light, OverlayTexture.NO_OVERLAY, null, -1, null, outline);
            poses.popPose();
        }

        private record Anchor(String name, List<ModelPart> path, ModelPart.Cube cube) {}
        private record Anchors(Anchor head, Anchor body) {}

        private static Anchors findAnchors(ModelPart root) {
            var parts = new ArrayList<Anchor>();
            collect(root, "root", new ArrayList<>(), parts);
            Anchor largest = parts.stream().max(Comparator.comparingDouble(a -> volume(a.cube()))).orElse(null);
            Anchor body = named(parts, List.of("body", "upper_body", "torso", "chest", "shell", "main"), largest);
            Anchor head = named(parts, List.of("head", "head_main", "center_head", "middle_head"), body);
            return new Anchors(head, body);
        }

        private static Anchor named(List<Anchor> parts, List<String> names, Anchor fallback) {
            for (String name : names)
                for (Anchor anchor : parts) if (name.equals(anchor.name())) return anchor;
            return fallback;
        }

        private static void collect(ModelPart part, String name, List<ModelPart> parents, List<Anchor> result) {
            var path = new ArrayList<>(parents);
            path.add(part);
            var access = (NativeCorpseModelPartAccessor) (Object) part;
            access.corpseorigin$cubes().stream().filter(c -> volume(c) > 0)
                    .max(Comparator.comparingDouble(Organs::volume))
                    .ifPresent(c -> result.add(new Anchor(name, List.copyOf(path), c)));
            access.corpseorigin$children().forEach((childName, child) -> collect(child, childName, path, result));
        }

        private static double volume(ModelPart.Cube c) {
            return (c.maxX - c.minX) * (c.maxY - c.minY) * (c.maxZ - c.minZ);
        }
    }
}
