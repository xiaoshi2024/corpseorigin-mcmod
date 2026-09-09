package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;

public class ExoskeletonRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final Identifier EXOSKELETON_TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_eye.png");

    private final ExoskeletonModel model;

    public ExoskeletonRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                                  ExoskeletonModel model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {

        // ✅ 只渲染尸兄玩家
        CorpseOriginClient.ClientCorpseData corpseData = CorpseOriginClient.corpseDataCache.get(state.id);
        if (corpseData == null || !corpseData.isCorpse || corpseData.isDisguised()) {
            return;
        }

        var parentModel = this.getParentModel();
        if (parentModel == null) return;

        model.copyFromHead(parentModel.head);
        model.setupAnim(state);

        poseStack.pushPose();

        // ✅ 使用 submitModelPart 提交模型部件
        // 或者直接使用 order().submitModelPart
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
}