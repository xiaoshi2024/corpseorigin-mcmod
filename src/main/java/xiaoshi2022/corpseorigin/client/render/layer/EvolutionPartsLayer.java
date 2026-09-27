package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;

/** Additional skeletal parts anchored to the animated player torso; never replaces the skin. */
public final class EvolutionPartsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final ModelPart leftWing, rightWing, gills;
    private static ModelPart bake(String name, CubeListBuilder cubes) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(name, cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild(name);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public EvolutionPartsLayer(RenderLayerParent parent) {
        super(parent);
        leftWing = bake("wing_left", CubeListBuilder.create()
                .addBox(1, 1, 2.5f, 13, 1, 1).addBox(3, 2, 2.7f, 10, 4, .6f)
                .addBox(4, 6, 2.7f, 7, 4, .6f).addBox(5, 10, 2.7f, 4, 3, .6f));
        rightWing = bake("wing_right", CubeListBuilder.create()
                .addBox(-14, 1, 2.5f, 13, 1, 1).addBox(-13, 2, 2.7f, 10, 4, .6f)
                .addBox(-11, 6, 2.7f, 7, 4, .6f).addBox(-9, 10, 2.7f, 4, 3, .6f));
        var fins = CubeListBuilder.create();
        for (int i = 0; i < 3; i++) {
            fins.addBox(-4.8f, 2 + i * 2, -1, .8f, 1, 4);
            fins.addBox(4, 2 + i * 2, -1, .8f, 1, 4);
        }
        gills = bake("gills", fins);
    }
    private void draw(ModelPart part, PoseStack poses, SubmitNodeCollector collector, int light, String texture) {
        collector.submitModelPart(part, poses, RenderTypes.entityCutout(CorpseOrigin.id(texture)),
                light, OverlayTexture.NO_OVERLAY, null);
    }
    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                                 AvatarRenderState state, float yaw, float pitch) {
        var level = Minecraft.getInstance().level;
        if (level == null || state.isInvisible || !(level.getEntity(state.id) instanceof Player player)) return;
        if (xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.disguised(player)) return;
        // Use the existing synchronized corpse cache, since PLAYER_CORPSE itself is server-only.
        var corpse = xiaoshi2022.corpseorigin.client.CorpseOriginClient.corpseDataCache.get(player.getUUID());
        if (corpse == null || !corpse.isCorpse || corpse.isDisguised()) return;
        poses.pushPose();
        getParentModel().body.translateAndRotate(poses);
        if (SurvivalGrowth.has(player, "wings")) {
            float flap = (float)Math.sin(player.tickCount * (player.onGround() ? .12 : .4)) * .35f;
            leftWing.yRot = .35f + flap;
            rightWing.yRot = -.35f - flap;
            draw(leftWing, poses, collector, light, "textures/entity/chapter_blood.png");
            draw(rightWing, poses, collector, light, "textures/entity/chapter_blood.png");
        }
        if (SurvivalGrowth.has(player, "gills"))
            draw(gills, poses, collector, light, "textures/entity/chapter_xuanwu.png");
        poses.popPose();
    }
}
