package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.client.model.clone.VoxelModel;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

import java.util.UUID;

public class CloneChamberRenderer
        implements BlockEntityRenderer<CloneChamberBlockEntity, CloneChamberRenderState> {

    // ===== 6 个预置 BlockState =====
    private static final BlockState STATE_BODY_LOWER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.BODY_LOWER);

    private static final BlockState STATE_BODY_UPPER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.BODY_UPPER);

    private static final BlockState STATE_DOOR_LEFT_LOWER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_LEFT_LOWER);

    private static final BlockState STATE_DOOR_LEFT_UPPER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_LEFT_UPPER);

    private static final BlockState STATE_DOOR_RIGHT_LOWER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_RIGHT_LOWER);

    private static final BlockState STATE_DOOR_RIGHT_UPPER =
            ModBlocks.CLONE_CHAMBER.defaultBlockState()
                    .setValue(CloneChamberBlock.PART, CloneChamberBlock.Part.DOOR_RIGHT_UPPER);

    /** 打印完成阈值，和 CloneState.COMPLETE_PROGRESS 一致 */
    private static final float COMPLETE_PROGRESS = 0.96F;

    private final BlockModelResolver modelResolver;
    private final PlayerModel cloneModel;
    private final VoxelModel voxelModel;

    public CloneChamberRenderer(BlockEntityRendererProvider.Context context) {
        this.modelResolver = context.blockModelResolver();

        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        this.cloneModel = new PlayerModel(
                entityModels.bakeLayer(ModModelLayers.CLONE_DUMMY), false);
        this.voxelModel = new VoxelModel(this.cloneModel);
    }

    @Override
    public CloneChamberRenderState createRenderState() {
        return new CloneChamberRenderState();
    }

    @Override
    public void extractRenderState(CloneChamberBlockEntity chamber, CloneChamberRenderState state,
                                   float partialTick, Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(chamber, state, overlay);
        state.lowerHalf = chamber.getBlockState().getValue(CloneChamberBlock.HALF) == DoubleBlockHalf.LOWER;
        state.facing = chamber.getBlockState().getValue(CloneChamberBlock.FACING);
        state.doorOpen = chamber.getDoorOpenProgress(partialTick);
        state.cloneProgress = chamber.getCloneProgress();
        state.hasClone = chamber.hasClone();
        state.ownerUuid = chamber.getOwnerUuid();
    }

    @Override
    public void submit(CloneChamberRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, CameraRenderState camera) {

        BlockDisplayContext displayContext = BlockDisplayContext.create();

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(state.facing.getOpposite().toYRot()));
        pose.translate(-0.5F, -0.5F, -0.5F);

        // ===== 1. 本体 =====
        BlockState bodyState = state.lowerHalf ? STATE_BODY_LOWER : STATE_BODY_UPPER;
        BlockModelRenderState bodyRenderState = new BlockModelRenderState();
        modelResolver.update(bodyRenderState, bodyState, displayContext);
        bodyRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // ===== 2. 门 =====
        float angle = state.doorOpen * 90.0F;

        // 右门：铰链 origin [15.5, 16, 1]
        pose.pushPose();
        pose.translate(15.5F / 16.0F, 0.0F, 1.0F / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-15.5F / 16.0F, 0.0F, -1.0F / 16.0F);
        BlockState rightDoorState = state.lowerHalf ? STATE_DOOR_RIGHT_LOWER : STATE_DOOR_RIGHT_UPPER;
        BlockModelRenderState rightDoorRenderState = new BlockModelRenderState();
        modelResolver.update(rightDoorRenderState, rightDoorState, displayContext);
        rightDoorRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // 左门：铰链 origin [1.5, 16, 1]
        pose.pushPose();
        pose.translate(1.5F / 16.0F, 0.0F, 1.0F / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-angle));
        pose.translate(-1.5F / 16.0F, 0.0F, -1.0F / 16.0F);
        BlockState leftDoorState = state.lowerHalf ? STATE_DOOR_LEFT_LOWER : STATE_DOOR_LEFT_UPPER;
        BlockModelRenderState leftDoorRenderState = new BlockModelRenderState();
        modelResolver.update(leftDoorRenderState, leftDoorState, displayContext);
        leftDoorRenderState.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // ===== 3. 克隆人（只在下半格渲染一次） =====
        if (state.lowerHalf && state.hasClone) {
            renderClone(pose, collector, state);
        }

        pose.popPose();
    }

    // ==================== 克隆人渲染 ====================

    private void renderClone(PoseStack pose, SubmitNodeCollector collector, CloneChamberRenderState state) {
        float progress = state.cloneProgress;
        PlayerSkin skin = resolveSkin(state.ownerUuid);

        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);   // ★ 翻正后往上推 1.5 格

        if (progress < COMPLETE_PROGRESS) {
            this.voxelModel.completeness = progress / COMPLETE_PROGRESS;

            collector.submitCustomGeometry(
                    pose,
                    RenderTypes.entityCutout(skin.body().texturePath()),
                    (poseEntry, consumer) -> {
                        PoseStack local = new PoseStack();
                        local.last().pose().set(poseEntry.pose());
                        local.last().normal().set(poseEntry.normal());
                        this.voxelModel.render(local, consumer,
                                state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
                    });
        } else {
            AvatarRenderState avatar = new AvatarRenderState();
            avatar.skin = skin;
            avatar.lightCoords = state.lightCoords;
            avatar.isSpectator = false;
            avatar.showHat = true;
            avatar.showJacket = true;
            avatar.showLeftPants = true;
            avatar.showRightPants = true;
            avatar.showLeftSleeve = true;
            avatar.showRightSleeve = true;
            avatar.showCape = false;

            collector.submitModel(
                    this.cloneModel,
                    avatar,
                    pose,
                    RenderTypes.entityTranslucent(skin.body().texturePath()),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    null);
        }

        pose.popPose();
    }

    private static PlayerSkin resolveSkin(UUID ownerUuid) {
        if (ownerUuid != null) {
            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                var info = conn.getPlayerInfo(ownerUuid);
                if (info != null) {
                    return info.getSkin();
                }
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }
}