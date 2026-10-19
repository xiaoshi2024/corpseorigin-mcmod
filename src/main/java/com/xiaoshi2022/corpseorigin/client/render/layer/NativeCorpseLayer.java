package com.xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.corpseorigin.client.render.NativeCorpseOrgans;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import com.xiaoshi2022.corpseorigin.mixin.NativeCorpseModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.AgeableListModel;
import com.xiaoshi2022.corpseorigin.mixin.NativeCorpseAgeableAccessor;
import com.xiaoshi2022.corpseorigin.client.render.NativeCorpseGeometry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 感染后叠加骨骼/器官渲染层（1.20.1 Forge）。
 * <p>
 * 不替换、不隐藏宿主模型，只在原模型之上叠加三层：
 * <ol>
 *   <li>veins：递归遍历宿主模型所有 ModelPart，用静脉贴图把每个 cube 再画一遍（深度偏移避免 z-fighting）</li>
 *   <li>ribs：找到 body/torso 锚点，在锚点 cube 的位置叠加静态肋骨模型</li>
 *   <li>eye：找到 head 锚点，在锚点 cube 的位置叠加眼睛模型</li>
 * </ol>
 * 锚点定位参考 26.2 Fabric 版：按名称优先匹配，失败则回退到体积最大的主体部件。
 */
public class NativeCorpseLayer extends RenderLayer<LivingEntity, EntityModel<LivingEntity>> {

    private static final List<String> BODY_NAMES =
            List.of("body", "upper_body", "torso", "chest", "shell", "main", "waist");
    private static final List<String> HEAD_NAMES =
            List.of("head", "head_main", "center_head", "middle_head");

    /**
     * 外层装饰部件（帽子、帽檐、外套、壳）。
     * 静脉不画在这些壳上（否则会在原版外观上「贴一层皮」），
     * 器官则要放到壳的外侧，避免被壳挡住造成穿模。
     */
    private static boolean isHiddenPart(String lowerName) {
        return lowerName.contains("hat") || lowerName.contains("brim");
    }

    /** 判断是否为器官外层壳（用于把器官放到最外侧）。 */
    private static boolean isOverlayShell(String lowerName) {
        return lowerName.contains("hat") || lowerName.contains("brim")
                || lowerName.contains("jacket") || lowerName.contains("robe")
                || lowerName.contains("shell") || lowerName.contains("coat");
    }

    private final Map<EntityModel<?>, List<Root>> rootsByModel = new WeakHashMap<>();
    private final Map<ModelPart, Anchors> anchorsByRoot = new IdentityHashMap<>();
    private final Map<ModelPart, ModelPart> surfaces = new IdentityHashMap<>();
    private final Map<ModelPart, NativeCorpseGeometry.Bounds> organBounds = new IdentityHashMap<>();

    /** 【临时诊断】每种实体只打印一次。 */
    private static final java.util.Set<String> DIAG_LOGGED =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public NativeCorpseLayer(RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       LivingEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        EntityModel<LivingEntity> model = getParentModel();
        List<Root> modelRoots = roots(model);
        boolean transformed = !entity.isInvisible() && NativeCorpseState.transformed(entity);
        if (DIAG_LOGGED.add(entity.getType().toString())) {
            List<String> rootNames = new ArrayList<>();
            for (Root root : modelRoots) rootNames.add(root.name());
            Anchors diagAnchors = findAnchors(model, modelRoots);
            com.xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.info(
                    "[diag2] {} model={} transformed={} mask={} roots={} body={} head={}",
                    entity.getType(), model.getClass().getSimpleName(), transformed,
                    NativeCorpseState.renderAppearance(entity), rootNames,
                    diagAnchors.body() == null ? "null" : diagAnchors.body().name(),
                    diagAnchors.head() == null ? "null" : diagAnchors.head().name());
        }
        if (!transformed) return;
        if (modelRoots.isEmpty()) return;

        // 1) 静脉：整个模型覆盖一层
        VertexConsumer veinsBuffer = bufferSource.getBuffer(RenderType.entityCutoutNoCullZOffset(NativeCorpseOrgans.VEINS));
        for (Root root : modelRoots) {
            poseStack.pushPose();
            applyAgeTransform(model, root.part(), poseStack);
            renderVeins(root.part(), root.name(), poseStack, veinsBuffer, packedLight);
            poseStack.popPose();
        }

        int mask = NativeCorpseState.renderAppearance(entity);
        if (mask == 0) return;

        // 2/3) 肋骨 + 眼睛：按锚点叠加
        Anchors found = anchorsByRoot.computeIfAbsent(modelRoots.get(0).part(), key -> findAnchors(model, modelRoots));
        if ((mask & NativeCorpseState.RIBS) != 0) {
            attach(found.body(), NativeCorpseOrgans.ribs(), false, poseStack, bufferSource, packedLight);
        }
        if ((mask & NativeCorpseState.EYE) != 0) {
            attach(found.head(), NativeCorpseOrgans.eye(), true, poseStack, bufferSource, packedLight);
        }
    }

    // ==================== 静脉层 ====================

    private void renderVeins(ModelPart part, String name, PoseStack poses, VertexConsumer buffer, int light) {
        if (!part.visible) return;
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (isHiddenPart(lower)) return;

        poses.pushPose();
        part.translateAndRotate(poses);

        NativeCorpseModelPartAccessor access = (NativeCorpseModelPartAccessor) (Object) part;
        List<ModelPart.Cube> cubes = access.corpseorigin$cubes();
        if (!part.skipDraw && !cubes.isEmpty()) {
            ModelPart surface = surfaces.computeIfAbsent(part, ignored -> new ModelPart(cubes, Map.of()));
            // 深度偏移由 RenderType 处理，保持每个子骨骼的原始变换。

            surface.render(poses, buffer, light, OverlayTexture.NO_OVERLAY);
        }
        access.corpseorigin$children()
                .forEach((childName, child) -> renderVeins(child, childName, poses, buffer, light));
        poses.popPose();
    }

    // ==================== 器官附着 ====================

    private void attach(Anchor anchor, ModelPart organ, boolean isEye, PoseStack poses,
                        MultiBufferSource bufferSource, int light) {
        if (anchor == null) return;
        for (ModelPart part : anchor.path()) {
            if (!part.visible) return;
        }
        ModelPart joint = anchor.path().get(anchor.path().size() - 1);
        if (joint.skipDraw) return;

        ModelPart.Cube cube = anchor.cube();
        NativeCorpseGeometry.Bounds box = NativeCorpseGeometry.bounds(cube, new PoseStack());
        float width = Math.max(0.5F, box.maxX() - box.minX());
        float height = Math.max(0.5F, box.maxY() - box.minY());
        // Ribs sit on the surface; the corpse eye belongs inside the skull.
        float frontZ = isEye ? box.minZ() : outermostFrontZ(joint, box.minZ());

        poses.pushPose();
        // 包含全部祖先，四足 / 嵌套头 / 幼年体都能保持自身姿态
        applyAgeTransform(getParentModel(), anchor.path().get(0), poses);
        for (ModelPart part : anchor.path()) {
            part.translateAndRotate(poses);
        }
        NativeCorpseGeometry.Bounds organBox = organBounds.computeIfAbsent(organ, NativeCorpseGeometry::bounds);
        if (isEye) {
            NativeCorpseGeometry.Placement sprout = NativeCorpseGeometry.sproutingEye(box);
            poses.translate(sprout.x() / 16, sprout.y() / 16, sprout.z() / 16);
            poses.scale(sprout.scale(), sprout.scale(), sprout.scale());
        } else {
            float scaleZ = Math.min(width, height) / 8.0F;
            poses.translate((box.minX() + box.maxX()) / 32.0F,
                    (box.minY() + height * 0.5F) / 16.0F,
                    (frontZ - 0.05F - organBox.maxZ() * scaleZ) / 16.0F);
            poses.scale(width / 9.0F, height / 12.0F, scaleZ);
        }
        organ.render(poses,
                bufferSource.getBuffer(isEye
                        ? RenderType.entityTranslucent(NativeCorpseOrgans.EYE)
                        : RenderType.entityCutoutNoCull(NativeCorpseOrgans.TISSUE)),
                light, OverlayTexture.NO_OVERLAY);
        poses.popPose();
    }

    /**
     * 找出锚点直接子部件里最外层壳（hat/jacket/robe 等）的前表面 Z。
     * 在锚点空间测量实际顶点，包含外扩、偏移和旋转。
     */
    private static float outermostFrontZ(ModelPart joint, float fallback) {
        float result = fallback;
        NativeCorpseModelPartAccessor access = (NativeCorpseModelPartAccessor) (Object) joint;
        for (Map.Entry<String, ModelPart> entry : access.corpseorigin$children().entrySet()) {
            ModelPart child = entry.getValue();
            String name = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            if (!isOverlayShell(name)) continue;
            if (!child.visible || child.skipDraw) continue;
            PoseStack shellPose = new PoseStack();
            child.translateAndRotate(shellPose);
            for (ModelPart.Cube cube : ((NativeCorpseModelPartAccessor) (Object) child).corpseorigin$cubes()) {
                result = Math.min(result, NativeCorpseGeometry.bounds(cube, shellPose).minZ());
            }
        }
        return result;
    }

    // ==================== 模型根部件收集 ====================

    private record Root(String name, ModelPart part) {}

    private List<Root> roots(EntityModel<?> model) {
        List<Root> cached = rootsByModel.get(model);
        if (cached != null) return cached;

        List<Root> candidates = new ArrayList<>();
        if (model instanceof HumanoidModel<?> humanoid) {
            candidates.add(new Root("head", humanoid.head));
            candidates.add(new Root("body", humanoid.body));
            candidates.add(new Root("right_arm", humanoid.rightArm));
            candidates.add(new Root("left_arm", humanoid.leftArm));
            candidates.add(new Root("right_leg", humanoid.rightLeg));
            candidates.add(new Root("left_leg", humanoid.leftLeg));
            candidates.add(new Root("hat", humanoid.hat));
        } else if (model instanceof net.minecraft.client.model.HierarchicalModel<?> hierarchical) {
            // 苦力怕 / 村民 / 铁傀儡等：直接拿模型根，避免依赖字段反射
            candidates.add(new Root("root", hierarchical.root()));
        } else if (model instanceof AgeableListModel<?>) {
            NativeCorpseAgeableAccessor ageable = (NativeCorpseAgeableAccessor) model;
            int index = 0;
            for (ModelPart part : ageable.corpseorigin$headParts())
                candidates.add(new Root(index++ == 0 ? "head" : "head_extra", part));
            index = 0;
            for (ModelPart part : ageable.corpseorigin$bodyParts())
                candidates.add(new Root(index++ == 0 ? "body" : "limb", part));
        } else {
            collectModelPartFields(model, candidates);
        }

        // 去掉「是别的根的后代」的重复根：
        // 村民这类 HierarchicalModel 同时持有 root 和 head/body 字段，
        // 否则同一部件会被静脉层画两次。
        List<Root> list = new ArrayList<>();
        for (Root candidate : candidates) {
            boolean nested = false;
            for (Root other : candidates) {
                if (other != candidate && containsPart(other.part(), candidate.part())) {
                    nested = true;
                    break;
                }
            }
            if (!nested) list.add(candidate);
        }
        rootsByModel.put(model, list);
        return list;
    }

    /** target 是否为 root 的（任意深度）后代。 */
    private static boolean containsPart(ModelPart root, ModelPart target) {
        NativeCorpseModelPartAccessor access = (NativeCorpseModelPartAccessor) (Object) root;
        for (ModelPart child : access.corpseorigin$children().values()) {
            if (child == target || containsPart(child, target)) return true;
        }
        return false;
    }

    /** 对非 HumanoidModel 的模型，反射收集所有 ModelPart 字段（四足、鱼、鸟等）。 */
    private static void collectModelPartFields(EntityModel<?> model, List<Root> out) {
        Set<ModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!ModelPart.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    if (field.get(model) instanceof ModelPart part && seen.add(part)) {
                        out.add(new Root(field.getName(), part));
                    }
                } catch (Throwable ignored) {
                    // 模块访问受限时跳过，其它字段仍可工作
                }
            }
        }
    }

    // ==================== 锚点定位 ====================

    private record Anchor(String name, List<ModelPart> path, ModelPart.Cube cube) {}

    private record Anchors(Anchor head, Anchor body) {}

    private static Anchors findAnchors(EntityModel<?> model, List<Root> roots) {
        List<Anchor> parts = new ArrayList<>();
        Set<ModelPart> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Root root : roots) {
            collectAnchors(root.part(), root.name(), List.of(), parts, visited);
        }
        Anchor largest = parts.stream()
                .max((a, b) -> Double.compare(volume(a.cube()), volume(b.cube())))
                .orElse(null);
        // CreeperModel does not retain a body field; bind using the baked child names.
        if (model instanceof CreeperModel<?> creeper) {
            ModelPart root = creeper.root();
            return new Anchors(anchorFor(parts, root.getChild("head")),
                    anchorFor(parts, root.getChild("body")));
        }
        Anchor body = named(parts, BODY_NAMES, largest);
        Anchor head = named(parts, HEAD_NAMES, null);
        if (head == null) head = body;
        return new Anchors(head, body);
    }

    private static Anchor anchorFor(List<Anchor> parts, ModelPart joint) {
        return parts.stream().filter(a -> a.path().get(a.path().size() - 1) == joint)
                .findFirst().orElse(null);
    }

    private static void applyAgeTransform(EntityModel<?> model, ModelPart root, PoseStack poses) {
        if (!model.young || !(model instanceof AgeableListModel<?>)) return;
        NativeCorpseAgeableAccessor ageable = (NativeCorpseAgeableAccessor) model;
        for (ModelPart head : ageable.corpseorigin$headParts()) {
            if (head != root) continue;
            if (ageable.corpseorigin$scaleHead()) {
                float scale = 1.5F / ageable.corpseorigin$babyHeadScale();
                poses.scale(scale, scale, scale);
            }
            poses.translate(0, ageable.corpseorigin$babyYHeadOffset() / 16.0F,
                    ageable.corpseorigin$babyZHeadOffset() / 16.0F);
            return;
        }
        float scale = 1.0F / ageable.corpseorigin$babyBodyScale();
        poses.scale(scale, scale, scale);
        poses.translate(0, ageable.corpseorigin$bodyYOffset() / 16.0F, 0);
    }
    private static Anchor named(List<Anchor> parts, List<String> names, Anchor fallback) {
        for (String name : names) {
            for (Anchor anchor : parts) {
                if (anchor.name() != null && name.equals(anchor.name().toLowerCase(Locale.ROOT))) return anchor;
            }
        }
        return fallback;
    }

    private static void collectAnchors(ModelPart part, String name, List<ModelPart> parents,
                                       List<Anchor> out, Set<ModelPart> visited) {
        if (!visited.add(part)) return;
        List<ModelPart> path = new ArrayList<>(parents);
        path.add(part);

        NativeCorpseModelPartAccessor access = (NativeCorpseModelPartAccessor) (Object) part;
        ModelPart.Cube largest = null;
        double best = 0.0;
        for (ModelPart.Cube cube : access.corpseorigin$cubes()) {
            double volume = volume(cube);
            if (volume > best) {
                best = volume;
                largest = cube;
            }
        }
        if (largest != null) out.add(new Anchor(name, List.copyOf(path), largest));

        access.corpseorigin$children().forEach((childName, child) ->
                collectAnchors(child, childName, path, out, visited));
    }

    private static double volume(ModelPart.Cube cube) {
        return (cube.maxX - cube.minX) * (cube.maxY - cube.minY) * (cube.maxZ - cube.minZ);
    }
}
