package xiaoshi2022.corpseorigin.client.soul;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

/**
 * 灵魂残影的渲染：在世界空间里画一具半透明、幽蓝发光的玩家模型，位置跟着灵魂摄像机走。
 * <p>
 * 模型用的是和仓内克隆人同一套 {@link PlayerModel}（{@link ModModelLayers#CLONE_DUMMY}），
 * 姿势/翻转也沿用原版实体渲染那套变换，只是渲染层换成自发光半透明、并叠一层蓝色 tint。
 */
@Environment(EnvType.CLIENT)
public final class SoulProjectionRenderer {

    /** 蓝色 tint：RGB 偏冷 + alpha 逐帧来自残影当前透明度 */
    private static final int GHOST_RGB = 0x9AD8FF;
    /** 灵魂不受场景光照影响，给全亮 */
    private static final int FULL_BRIGHT = 0xF000F0;
    /** 原版活体模型脚底补偿量（模型坐标系 y 翻转后把脚底抬回原点） */
    private static final float MODEL_LIFT = -1.501F;

    private static PlayerModel model;

    private SoulProjectionRenderer() {
    }

    public static void render(PoseStack stack, SubmitNodeCollector collector) {
        float alpha = SoulProjectionClient.ghostAlpha();
        if (alpha <= 0.02F) {
            return;
        }
        Vec3 pos = SoulProjectionClient.ghostPosition();
        if (pos == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        Vec3 cameraPos = client.gameRenderer.mainCamera().position();
        if (pos.distanceToSqr(cameraPos) > 4096.0) {
            return;
        }

        PlayerModel model = model(client);
        PlayerSkin skin = player.getSkin();

        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        state.lightCoords = FULL_BRIGHT;
        state.scale = 1.0F;
        state.bodyRot = SoulProjectionClient.ghostBodyRot();
        // 原版 yRot 是「头相对身体的偏移」，身体朝向已经在 PoseStack 里转过了，这里让头自然朝前
        state.yRot = 0.0F;
        state.xRot = SoulProjectionClient.ghostPitch();
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.isSpectator = false;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
        state.showExtraEars = false;

        int tint = ARGB.color(alpha, GHOST_RGB);

        stack.pushPose();
        // COLLECT_SUBMITS 给的是相机坐标系，先挪回世界原点再放到灵魂位置上
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        stack.translate(pos.x, pos.y, pos.z);
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, MODEL_LIFT, 0.0F);

        model.setupAnim(state);
        collector.submitModel(
                model,
                state,
                stack,
                RenderTypes.entityTranslucentEmissive(skin.body().texturePath()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                tint,
                null);

        stack.popPose();
    }

    private static PlayerModel model(Minecraft client) {
        if (model == null) {
            model = new PlayerModel(client.getEntityModels().bakeLayer(ModModelLayers.CLONE_DUMMY), false);
        }
        return model;
    }
}
