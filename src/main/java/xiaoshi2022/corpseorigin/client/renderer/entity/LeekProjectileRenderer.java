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
import xiaoshi2022.corpseorigin.entity.LeekProjectileEntity;

/**
 * 投掷大葱渲染器 —— 参照 {@link FlyingGreatSwordRenderer} 的物品渲染方式，
 * 用竹节（BAMBOO）物品模型代替大葱，直到有正式的大葱模型。
 */
public class LeekProjectileRenderer
        extends EntityRenderer<LeekProjectileEntity, LeekProjectileRenderState> {

    private final ItemModelResolver itemModelResolver;

    public LeekProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.0f;
    }

    @Override
    public LeekProjectileRenderState createRenderState() {
        return new LeekProjectileRenderState();
    }

    @Override
    public void extractRenderState(LeekProjectileEntity entity,
                                   LeekProjectileRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.itemStack = entity.getItemStack();
        state.syncedYaw = entity.getSyncedYaw();
        state.syncedPitch = entity.getSyncedPitch();
        state.sourceEntity = entity;
    }

    @Override
    public void submit(LeekProjectileRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        if (state.itemStack.isEmpty() || state.sourceEntity == null) {
            return;
        }

        poseStack.pushPose();

        // ① 飞行方向朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(state.syncedYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.syncedPitch));

        // ② 平面物品（generated 模型）沿飞行方向放倒
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));

        // ③ 缩放
        poseStack.scale(1.2f, 1.2f, 1.2f);

        // ④ 渲染
        ItemStackRenderState renderState = new ItemStackRenderState();
        this.itemModelResolver.updateForNonLiving(
                renderState, state.itemStack, ItemDisplayContext.FIXED, state.sourceEntity);
        renderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
