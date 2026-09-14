package xiaoshi2022.corpseorigin.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.block.entity.ShellStorageBlockEntity;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

public class ShellStorageRenderer
        implements BlockEntityRenderer<ShellStorageBlockEntity, ShellStorageRenderState> {

    private final PlayerModel model;

    public ShellStorageRenderer(BlockEntityRendererProvider.Context context) {
        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        this.model = new PlayerModel(
                entityModels.bakeLayer(ModModelLayers.CLONE_DUMMY), false);
    }

    @Override
    public ShellStorageRenderState createRenderState() {
        return new ShellStorageRenderState();
    }

    @Override
    public void extractRenderState(ShellStorageBlockEntity be, ShellStorageRenderState state,
                                   float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(be, state, overlay);

        var stored = be.getStoredState();
        state.hasBody = stored != null && stored.getOwnerUuid() != null;
        state.ownerUuid = state.hasBody ? stored.getOwnerUuid() : null;
        state.progress = stored == null ? 0.0F : stored.getProgress();

        if (be.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            state.facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }
    }

    @Override
    public void submit(ShellStorageRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hasBody) {
            return;
        }

        PlayerSkin skin = ClientSkinCache.resolve(state.ownerUuid);

        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(state.facing.getOpposite().toYRot()));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);

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
                this.model,
                avatar,
                pose,
                RenderTypes.entityTranslucent(skin.body().texturePath()),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                null);

        pose.popPose();
    }
}