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

import java.util.UUID;

public class ExoskeletonRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final Identifier EXOSKELETON_TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_eye.png");

    private final ExoskeletonModel model;

    /**
     * ✅ 用原始类型构造函数，兼容 AvatarRendererMixin 的调用
     */
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

        CorpseOriginClient.ClientCorpseData corpseData = CorpseOriginClient.corpseDataCache.get(uuid);
        if (corpseData == null || !corpseData.isCorpse || corpseData.isDisguised()) {
            return;
        }

        var parentModel = this.getParentModel();
        if (parentModel == null) return;

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

    private UUID getEntityUuid(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;

        Entity entity = client.level.getEntity(entityId);
        return entity != null ? entity.getUUID() : null;
    }

    public void triggerSwing() {
        model.triggerSwing();
    }
}