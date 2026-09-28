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
 * 闄勫姞楠ㄩ锛堢繀鑶€ / 楸奸硟锛夛紝閿氬湪甯﹀姩鐢荤殑浜哄舰韬共涓婏紝缁濅笉鏇挎崲鐨偆銆? * <p>
 * 鐪熺帺瀹惰鑷繁鍚屾鐨?{@code evolution_parts} 闄勪欢锛涘厠闅嗗垎韬紙瀹炰綋 / 浠撳唴鎵嬬粯涓嶈蛋杩欏眰锛? * 璇?{@code CloneBodySyncS2C} 鍚屾杩囨潵鐨勮鑹插瑙傜紦瀛?鈥斺€?涓よ竟閮芥槸绾?ModelPart 鎵嬬粯锛? * 涓嶄緷璧?GeckoLib 鐨勭帺瀹?animatable 閫氶亾銆? */
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

    /** 杩欎竴灞傝鐢荤殑杩涘寲閮ㄤ欢鏉ユ簮锛氱湡鐜╁鐢ㄩ檮浠讹紝鍒嗚韩鐢ㄧ綉缁滅紦瀛樸€?*/
    private record PartsSource(Entity entity, CompoundTag body, xiaoshi2022.corpseorigin.client.ClientCorpseData corpse) {}

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
            // 娌℃湁瑙掕壊澶栬鍖呮椂锛堟棫鏁版嵁 / 灏氭湭閫佽揪锛変笉鐢荤繀鑶€锛岀瓑涓嬩竴娆″悓姝ヨ嚜鎰?            return cloneBody == null ? null
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
