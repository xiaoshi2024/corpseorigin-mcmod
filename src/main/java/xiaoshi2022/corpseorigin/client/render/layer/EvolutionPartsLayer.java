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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;

/**
 * 附加骨骼（翅膀 / 鱼鳃），锚在带动画的人形躯干上，绝不替换皮肤。
 * <p>
 * 真玩家读自己同步的 {@code evolution_parts} 附件；克隆分身（实体 / 仓内手绘不走这层）
 * 读 {@code CloneBodySyncS2C} 同步过来的角色外观缓存 —— 两边都是纯 ModelPart 手绘，
 * 不依赖 GeckoLib 的玩家 animatable 通道。
 */
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

    /** 这一层要画的进化部件来源：真玩家用附件，分身用网络缓存。 */
    private record PartsSource(Entity entity, CompoundTag body, CorpseOriginClient.ClientCorpseData corpse) {}

    private static PartsSource resolve(Entity entity) {
        var corpse = CorpseOriginClient.corpseDataCache.get(entity.getUUID());
        if (corpse == null || !corpse.isCorpse || corpse.isDisguised()) {
            return null;
        }
        if (entity instanceof Player player) {
            if (xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.disguised(player)) {
                return null;
            }
            return new PartsSource(player, player.getAttachedOrCreate(SurvivalGrowth.BODY), corpse);
        }
        if (entity instanceof CloneAvatarEntity) {
            CorpseOriginClient.ClientCloneBody cloneBody =
                    CorpseOriginClient.cloneBodyDataCache.get(entity.getUUID());
            // 没有角色外观包时（旧数据 / 尚未送达）不画翅膀，等下一次同步自愈
            return cloneBody == null ? null
                    : new PartsSource(entity, cloneBody.evolutionParts(), corpse);
        }
        return null;
    }

    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                                 AvatarRenderState state, float yaw, float pitch) {
        var level = Minecraft.getInstance().level;
        if (level == null || state.isInvisible) return;
        Entity resolvedEntity = level.getEntity(state.id);
        if (resolvedEntity == null) return;
        PartsSource source = resolve(resolvedEntity);
        if (source == null) return;
        CompoundTag body = source.body();
        boolean wings = body.getBooleanOr("wings", false);
        boolean hasGills = body.getBooleanOr("gills", false);
        if (!wings && !hasGills) return;

        poses.pushPose();
        getParentModel().body.translateAndRotate(poses);
        if (wings) {
            float flap = (float)Math.sin(resolvedEntity.tickCount * (resolvedEntity.onGround() ? .12 : .4)) * .35f;
            leftWing.yRot = .35f + flap;
            rightWing.yRot = -.35f - flap;
            draw(leftWing, poses, collector, light, "textures/entity/chapter_blood.png");
            draw(rightWing, poses, collector, light, "textures/entity/chapter_blood.png");
        }
        if (hasGills) {
            draw(gills, poses, collector, light, "textures/entity/chapter_xuanwu.png");
        }
        poses.popPose();
    }
}
