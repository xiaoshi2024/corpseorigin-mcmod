package xiaoshi2022.corpseorigin.client.render.layer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.animation.ZbLayerAnimationCache;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.OrganSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 低阶尸兄的突变器官渲染层：把器官 GEO 模型挂到对应骨骼上，
 * 用各自渲染状态里的 CLIP 播器官自己的动画（动画控制器见实体 registerControllers）。
 * <p>
 * 器官来源与玩家一致：客户端目录 {@link OrganClient#catalog}（含资源包自定义器官）。
 */
public final class ZbOrganLayer extends GeoRenderLayer<LowerLevelZbEntity, Void, LivingEntityRenderState> {

    private static final DataTicket<List> FRAMES = DataTicket.create("zb_organ_frames", List.class);
    private static final OrganDefinition RIBS = new OrganDefinition("corpse_horror_ribs", "Exposed ribs", "cosmetic",
            "corpseorigin:geckolib/models/entity/corpse_ribs.geo.json", "corpseorigin:textures/entity/corpse_ribs.png",
            "corpseorigin:geckolib/animations/entity/corpse_ribs.animation.json", Map.of("idle", "idle"));

    private record Frame(OrganSlot slot, OrganRenderer renderer, LivingEntityRenderState state) {}

    private final EntityRendererProvider.Context context;
    private final Map<String, OrganRenderer> renderers = new HashMap<>();

    public ZbOrganLayer(EntityRendererProvider.Context context,
                        com.geckolib.renderer.base.GeoRenderer<LowerLevelZbEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
        this.context = context;
    }

    @Override
    public void addRenderData(LowerLevelZbEntity entity, Void ignored,
                              LivingEntityRenderState renderState, float partialTick) {
        var frames = new ArrayList<Frame>();
        List<OrganSlot> slots;
        try {
            slots = OrganLibrary.parseSlots(entity.getOrganLoadout());
        } catch (Exception e) {
            slots = List.of();
        }

        var resources = Minecraft.getInstance().getResourceManager();
        for (int i = 0; i < slots.size(); i++) {
            OrganSlot slot = slots.get(i);
            OrganDefinition def = OrganClient.catalog.stream()
                    .filter(d -> d.id().equals(slot.organ())).findFirst().orElse(null);
            if (def == null) continue;
            // 资源缺任一文件就跳过这只器官，不影响其他器官
            if (resources.getResource(Identifier.parse(def.model())).isEmpty()
                    || resources.getResource(Identifier.parse(def.texture())).isEmpty()
                    || resources.getResource(Identifier.parse(def.animation())).isEmpty()) continue;

            String key = def.id() + ":" + i;
            final int slotIndex = i;
            OrganRenderer renderer = renderers.computeIfAbsent(key, k -> new OrganRenderer(context, def, slotIndex));

            var organState = new LivingEntityRenderState();
            String motion = entity.isInWater() ? "swim"
                    : entity.swinging ? "attack"
                    : !entity.onGround() ? "fly"
                    : renderState.walkAnimationSpeed > 0.02F ? "walk"
                    : "idle";
            organState.addGeckolibData(ZbLayerAnimationCache.CLIP,
                    def.clips().getOrDefault(motion, def.clips().get("idle")));
            renderer.extractRenderState(entity, organState, partialTick);
            frames.add(new Frame(slot, renderer, organState));
        }
        if (xiaoshi2022.corpseorigin.growth.CorpseHorror.applies(entity)
                && entity.hasVisibleRibs()
                && slots.stream().noneMatch(OrganSlot::replacesBody)) {
            var renderer = renderers.computeIfAbsent(RIBS.id(), k -> new OrganRenderer(context, RIBS, -1));
            var state = new LivingEntityRenderState();
            state.addGeckolibData(ZbLayerAnimationCache.CLIP, "idle");
            renderer.extractRenderState(entity, state, partialTick);
            frames.add(new Frame(new OrganSlot(RIBS.id(), "body", 0, 0, 0, 0, 0, 0, 1, false), renderer, state));
        }
        renderState.addGeckolibData(FRAMES, List.copyOf(frames));
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info,
                                 java.util.function.BiConsumer<com.geckolib.cache.model.GeoBone, PerBoneRender<LivingEntityRenderState>> registrar) {
        if (!info.willRender()) return;
        List<?> frames = info.renderState().getOrDefaultGeckolibData(FRAMES, List.of());
        var model = info.model();
        for (Object o : frames) {
            Frame frame = (Frame) o;
            String boneName = boneForJoint(frame.slot().joint());
            model.getBone(boneName).ifPresent(bone -> registrar.accept(bone, (passInfo, geoBone, collector) -> {
                var poses = passInfo.poseStack();
                poses.pushPose();
                var slot = frame.slot();
                // 此时姿态已在骨头位置，以下都是骨骼局部空间
                poses.translate(slot.x() / 16.0F, slot.y() / 16.0F, slot.z() / 16.0F);
                poses.mulPose(Axis.XP.rotationDegrees(slot.rx()));
                poses.mulPose(Axis.YP.rotationDegrees(slot.ry()));
                poses.mulPose(Axis.ZP.rotationDegrees(slot.rz()));
                poses.scale(slot.scale(), slot.scale(), slot.scale());
                frame.renderer().performRenderPass(frame.state(), poses, collector, new CameraRenderState());
                poses.popPose();
            }));
        }
    }

    /**
     * 这份渲染状态里是否存在生效的全身替换器官。
     * <p>
     * 有的话 {@code LowerLevelZbRenderer} 就不再画本体，只留器官。
     */
    public static boolean replacesBody(LivingEntityRenderState state) {
        List<?> frames = state.getOrDefaultGeckolibData(FRAMES, List.of());
        return frames.stream().anyMatch(o -> o instanceof Frame f && f.slot().replacesBody());
    }

    /** 器官关节名 → 尸兄模型骨骼名。 */
    private static String boneForJoint(String joint) {
        return switch (joint) {
            case "head" -> "Head";
            case "left_arm" -> "Left Arm";
            case "right_arm" -> "Right Arm";
            case "left_leg" -> "Left Leg";
            case "right_leg" -> "Right Leg";
            // full_body 走 Body：该骨骼枢轴 [0,24,0]（= 离脚 1.5 格），
            // 与玩家全身替换的锚点一致，也是器官模型的作者坐标系原点。
            default -> "Body";
        };
    }

    // ==================== 器官模型 ====================

    private static final class OrganModel extends DefaultedEntityGeoModel<LowerLevelZbEntity> {
        private final OrganDefinition def;

        OrganModel(OrganDefinition def) {
            super(Identifier.fromNamespaceAndPath("corpseorigin", "organ_part"));
            this.def = def;
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return Identifier.parse(xiaoshi2022.corpseorigin.growth.OrganResourceIds.model(def.model()));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return Identifier.parse(def.texture());
        }

        @Override
        public Identifier getAnimationResource(LowerLevelZbEntity animatable) {
            return Identifier.parse(xiaoshi2022.corpseorigin.growth.OrganResourceIds.animation(def.animation()));
        }
    }

    // ==================== 器官渲染器 ====================

    private static final class OrganRenderer
            extends GeoReplacedEntityRenderer<LowerLevelZbEntity, LowerLevelZbEntity, LivingEntityRenderState> {

        private final int slot;

        OrganRenderer(EntityRendererProvider.Context context, OrganDefinition def, int slot) {
            super(context, new OrganModel(def), null);
            this.slot = slot;
            this.shadowRadius = 0;
        }

        @Override
        public long getInstanceId(LowerLevelZbEntity animatable, LowerLevelZbEntity entity) {
            return slot < 0 ? ZbLayerAnimationCache.HORROR_ID : ZbLayerAnimationCache.organId(slot);
        }

        @Override
        public LivingEntityRenderState fillRenderState(LowerLevelZbEntity animatable, LowerLevelZbEntity entity,
                                                       LivingEntityRenderState state, float partialTick) {
            // 构造器里 animatable 传的是 null，改用实体自身作动画宿主
            return super.fillRenderState(entity, entity, state, partialTick);
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<LivingEntityRenderState> info) {
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
        }
    }
}
