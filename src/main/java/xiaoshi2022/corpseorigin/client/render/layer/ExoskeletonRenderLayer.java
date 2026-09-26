package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExoskeletonRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final Identifier EXOSKELETON_TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_eye.png");

    /** ✅ 红眼叠加贴图 */
    public static final Identifier RED_EYE_OVERLAY =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/red_eye_overlay.png");

    /** ✅ 实体ID → UUID 缓存 */
    private static final Map<Integer, UUID> UUID_CACHE = new ConcurrentHashMap<>();

    private final ExoskeletonModel model;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ExoskeletonRenderLayer(RenderLayerParent parent, ExoskeletonModel model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {

        UUID uuid = getEntityUuid(state.id);
        if (uuid == null) return;

        Entity entity = Minecraft.getInstance().level == null
                ? null : Minecraft.getInstance().level.getEntity(state.id);

        var parentModel = this.getParentModel();
        if (parentModel == null) return;

        // ==================== 1. 尸兄器官渲染（原有逻辑） ====================
        // 分身的尸兄状态已按它自己的 uuid 同步过来，所以这里直接用实体 uuid
        // 无外骨骼通用变种（天线宝宝尸兄那种自带整套盔甲外观的、左护法变异体）算尸兄，但不长这根尸眼骨骼
        CorpseOriginClient.ClientCorpseData corpseData = CorpseOriginClient.corpseDataCache.get(uuid);
        if (corpseData != null && corpseData.isCorpse && !corpseData.isDisguised()
                && PlayerCorpseComponent.hasExoskeleton(corpseData.getVariant())) {
            model.copyFromHead(parentModel.head);
            model.setupAnim(state);

            poseStack.pushPose();
            submitNodeCollector.order(0).submitModelPart(
                    model.getShieye(),
                    poseStack,
                    RenderTypes.entityTranslucent(EXOSKELETON_TEXTURE),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    null
            );
            poseStack.popPose();
        }

        // ==================== 2. 红眼特效渲染（杀戮觉醒） ====================
        // 红眼是玩家自己的战斗状态（按玩家 uuid 记），分身跟着主人一起亮
        UUID redEyeUuid = uuid;
        if (entity instanceof xiaoshi2022.corpseorigin.entity.CloneAvatarEntity avatar
                && avatar.getOwnerUuid() != null) {
            redEyeUuid = avatar.getOwnerUuid();
        }
        int remain = CorpseOriginClient.tempRedEyeTicks.getOrDefault(redEyeUuid, 0);
        if (remain > 0) {
            poseStack.pushPose();

            submitNodeCollector.order(1).submitModelPart(
                    parentModel.head,                   // ✅ head 的 pose 一定是对的
                    poseStack,
                    RenderTypes.eyes(RED_EYE_OVERLAY),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    null
            );

            poseStack.popPose();
        }
    }

    private UUID getEntityUuid(int entityId) {
        UUID cached = UUID_CACHE.get(entityId);
        if (cached != null) return cached;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;

        Entity entity = client.level.getEntity(entityId);
        if (entity == null) return null;

        UUID uuid = entity.getUUID();
        UUID_CACHE.put(entityId, uuid);
        return uuid;
    }

    /** ✅ 实体卸载时清理 */
    public static void onEntityRemoved(int entityId) {
        UUID_CACHE.remove(entityId);
    }

    /** ✅ 清空所有缓存（玩家退出时） */
    public static void clearCache() {
        UUID_CACHE.clear();
    }

}
