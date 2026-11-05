package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.render.BoundPose;

/**
 * 被 {@code /zb_bind} 捆住的玩家 → 在原有皮肤、原有动画之上叠加绳索。
 * <p>
 * 姿态完全不改（躺平那套已删除）：只在胸口、腰、双手手腕、双脚脚踝各画一圈绳。
 * 绳子挂到对应的骨骼上（body / arm / leg），所以玩家走路、挥手臂时绳子跟着动，
 * 不做自由摆动，看起来就是"被绳子一圈圈绑住"。
 */
public class BoundRopeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final net.minecraft.resources.Identifier TEXTURE =
            CorpseOrigin.id("textures/entity/bound_rope.png");

    private final ModelPart chest, waist;
    private final ModelPart wristRight, wristRightSlim, wristLeft, wristLeftSlim;
    private final ModelPart ankleRight, ankleLeft;

    private static ModelPart part(CubeListBuilder cubes) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("part", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("part");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public BoundRopeLayer(RenderLayerParent parent) {
        super(parent);
        // 躯干：两根横向绳，略大于 8x12x4 的躯干，包在外面
        chest = part(CubeListBuilder.create().addBox(-4.35f, 3.6f, -2.35f, 8.7f, 1.3f, 4.7f));
        waist = part(CubeListBuilder.create().addBox(-4.35f, 7.6f, -2.35f, 8.7f, 1.3f, 4.7f));
        // 手腕：经典手臂 4 宽 / 细手臂 3 宽，左右各一套
        wristRight = part(CubeListBuilder.create().addBox(-3.2f, 7.8f, -2.2f, 4.4f, 1.3f, 4.4f));
        wristRightSlim = part(CubeListBuilder.create().addBox(-2.2f, 7.8f, -2.2f, 3.4f, 1.3f, 4.4f));
        wristLeft = part(CubeListBuilder.create().addBox(-1.2f, 7.8f, -2.2f, 4.4f, 1.3f, 4.4f));
        wristLeftSlim = part(CubeListBuilder.create().addBox(-1.2f, 7.8f, -2.2f, 3.4f, 1.3f, 4.4f));
        // 脚踝：双腿模型都是 addBox(-2,0,-2,4,12,4)
        ankleRight = part(CubeListBuilder.create().addBox(-2.2f, 9.2f, -2.2f, 4.4f, 1.3f, 4.4f));
        ankleLeft = part(CubeListBuilder.create().addBox(-2.2f, 9.2f, -2.2f, 4.4f, 1.3f, 4.4f));
    }

    private void draw(ModelPart rope, ModelPart anchor, PoseStack poses, SubmitNodeCollector collector, int light) {
        poses.pushPose();
        anchor.translateAndRotate(poses);
        collector.order(0).submitModelPart(rope, poses, RenderTypes.entityCutout(TEXTURE),
                light, OverlayTexture.NO_OVERLAY, null);
        poses.popPose();
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yaw, float pitch) {
        if (state.isInvisible) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var actor = level.getEntity(state.id);
        if (!(actor instanceof Player player) || !BoundPose.active(player)) return;

        var model = getParentModel();
        draw(chest, model.body, poses, collector, light);
        draw(waist, model.body, poses, collector, light);
        boolean slim = state.skin != null && state.skin.model() == PlayerModelType.SLIM;
        draw(slim ? wristRightSlim : wristRight, model.rightArm, poses, collector, light);
        draw(slim ? wristLeftSlim : wristLeft, model.leftArm, poses, collector, light);
        draw(ankleRight, model.rightLeg, poses, collector, light);
        draw(ankleLeft, model.leftLeg, poses, collector, light);
    }
}
