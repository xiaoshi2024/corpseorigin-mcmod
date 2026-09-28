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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExoskeletonRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final Identifier EXOSKELETON_TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_eye.png");

    /** 鉁?绾㈢溂鍙犲姞璐村浘 */
    public static final Identifier RED_EYE_OVERLAY =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/red_eye_overlay.png");

    /** 鉁?瀹炰綋ID 鈫?UUID 缂撳瓨 */
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
        if (entity instanceof net.minecraft.world.entity.player.Player player
                && xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.disguised(player)) return;

        var parentModel = this.getParentModel();
        if (parentModel == null) return;

        // ==================== 1. 灏稿厔鍣ㄥ畼娓叉煋锛堝師鏈夐€昏緫锛?====================
        // 鍒嗚韩鐨勫案鍏勭姸鎬佸凡鎸夊畠鑷繁鐨?uuid 鍚屾杩囨潵锛屾墍浠ヨ繖閲岀洿鎺ョ敤瀹炰綋 uuid
        // 鏃犲楠ㄩ閫氱敤鍙樼锛堝ぉ绾垮疂瀹濆案鍏勯偅绉嶈嚜甯︽暣濂楃洈鐢插瑙傜殑銆佸乏鎶ゆ硶鍙樺紓浣擄級绠楀案鍏勶紝浣嗕笉闀胯繖鏍瑰案鐪奸楠?
        xiaoshi2022.corpseorigin.client.ClientCorpseData corpseData = CorpseOriginClient.corpseDataCache.get(uuid);
        if (corpseData != null && corpseData.showsCorpseEye()) {
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

        // ==================== 2. 绾㈢溂鐗规晥娓叉煋锛堟潃鎴閱掞級 ====================
        // 绾㈢溂鏄帺瀹惰嚜宸辩殑鎴樻枟鐘舵€侊紙鎸夌帺瀹?uuid 璁帮級锛屽垎韬窡鐫€涓讳汉涓€璧蜂寒
        UUID redEyeUuid = uuid;
        if (entity instanceof xiaoshi2022.corpseorigin.entity.CloneAvatarEntity avatar
                && avatar.getOwnerUuid() != null) {
            redEyeUuid = avatar.getOwnerUuid();
        }
        int remain = CorpseOriginClient.tempRedEyeTicks.getOrDefault(redEyeUuid, 0);
        if (remain > 0) {
            poseStack.pushPose();

            submitNodeCollector.order(1).submitModelPart(
                    parentModel.head,                   // 鉁?head 鐨?pose 涓€瀹氭槸瀵圭殑
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

    /** 鉁?瀹炰綋鍗歌浇鏃舵竻鐞?*/
    public static void onEntityRemoved(int entityId) {
        UUID_CACHE.remove(entityId);
    }

    /** 鉁?娓呯┖鎵€鏈夌紦瀛橈紙鐜╁閫€鍑烘椂锛?*/
    public static void clearCache() {
        UUID_CACHE.clear();
    }

}
