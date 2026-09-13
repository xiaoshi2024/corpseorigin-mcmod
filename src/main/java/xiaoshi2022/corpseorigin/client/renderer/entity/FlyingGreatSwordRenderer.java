package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;

public class FlyingGreatSwordRenderer
        extends EntityRenderer<FlyingGreatSwordEntity, FlyingGreatSwordRenderState> {

    private final ItemModelResolver itemModelResolver;

    public FlyingGreatSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.5f;
    }

    @Override
    public FlyingGreatSwordRenderState createRenderState() {
        return new FlyingGreatSwordRenderState();
    }

    @Override
    public void extractRenderState(FlyingGreatSwordEntity entity,
                                   FlyingGreatSwordRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.itemStack = entity.getItemStack();
        state.syncedYaw = entity.getSyncedYaw();
        state.syncedPitch = entity.getSyncedPitch();
        state.roll = entity.getRenderRoll();
        state.modelYawOffset = entity.getModelYawOffset();
        state.modelPitchOffset = entity.getModelPitchOffset();
        state.renderScale = entity.getRenderScale();
        state.sourceEntity = entity;
    }

    @Override
    public void submit(FlyingGreatSwordRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        ItemStack stack = state.itemStack;
        if (stack.isEmpty() || state.sourceEntity == null || state.renderScale <= 0.001f) {
            return;
        }

        poseStack.pushPose();

        // ① 实体朝向（世界方向，飞行方向）
        poseStack.mulPose(Axis.YP.rotationDegrees(state.syncedYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.syncedPitch));

        // ② 模型修正
        poseStack.mulPose(Axis.XP.rotationDegrees(270f));    // 保持躺平
        poseStack.mulPose(Axis.ZP.rotationDegrees(225f));    // 45°抵斜置 + 180°翻前后 = 225°
        poseStack.mulPose(Axis.YP.rotationDegrees(state.modelYawOffset));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.modelPitchOffset));

        // ③ 缩放
        poseStack.scale(state.renderScale, state.renderScale, state.renderScale);

        // ④ 渲染
        ItemStackRenderState renderState = new ItemStackRenderState();
        this.itemModelResolver.updateForNonLiving(
                renderState, stack, ItemDisplayContext.FIXED, state.sourceEntity);
        renderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}