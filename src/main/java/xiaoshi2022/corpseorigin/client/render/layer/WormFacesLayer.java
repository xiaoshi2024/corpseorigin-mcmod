package xiaoshi2022.corpseorigin.client.render.layer;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.authlib.GameProfile;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinCache;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinIntegration;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.entity.MultiHeadCorpseWormEntity;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Draws a victim's head at each animated body segment. */
public final class WormFacesLayer extends GeoRenderLayer<MultiHeadCorpseWormEntity, Void, LivingEntityRenderState> {

    /** 每个 segment 要绘制的皮肤来源字符串。 */
    private static final DataTicket<String[]> FACES =
            DataTicket.create("worm_faces", String[].class);

    /** 渲染时用的随机种子，避免每帧抖动但每只虫子不同。 */
    private static final DataTicket<Long> RANDOM_SEED =
            DataTicket.create("worm_faces_seed", Long.class);

    private static final Identifier CORPSE_SKIN =
            Identifier.parse("minecraft:textures/entity/zombie/zombie.png");

    /** 已经发过请求的来源（p:UUID / n:name），避免重复异步拉取。 */
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();

    /** 最多渲染几个头，和几何文件里 segment_2 ~ segment_7 对齐。 */
    private static final int MAX_HEADS = 6;

    private final ModelPart head;

    public WormFacesLayer(GeoRenderer<MultiHeadCorpseWormEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8),
                PartPose.ZERO
        );
        this.head = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("head");
    }

    @Override
    public void addRenderData(MultiHeadCorpseWormEntity entity, Void ignored,
                              LivingEntityRenderState state, float partialTick) {
        String[] faces = entity.getFaceSkins();
        state.addGeckolibData(FACES, faces);

        // 用实体 UUID 作为种子，让每只虫子的偏移固定但各不相同。
        // 用 hashCode 转 long，避免占用实体 id（1.21+ 的 RenderState 不一定有 entityId）。
        long seed = entity.getUUID().getMostSignificantBits()
                ^ entity.getUUID().getLeastSignificantBits()
                ^ (long) faces.length * 0x9E3779B97F4A7C15L;
        state.addGeckolibData(RANDOM_SEED, seed);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info,
                                 BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> registrar) {
        if (!info.willRender()) return;

        String[] skins = info.renderState().getOrDefaultGeckolibData(FACES, new String[0]);
        if (skins.length == 0) return;

        long baseSeed = info.renderState().getOrDefaultGeckolibData(RANDOM_SEED, 0L);

        int count = Math.min(MAX_HEADS, skins.length);
        for (int i = 0; i < count; i++) {
            final int index = i;
            final String skinSource = skins[i];

            info.model().getBone("segment_" + (i + 2)).ifPresent(bone ->
                    registrar.accept(bone, (pass, posedBone, collector) -> {
                        var pose = pass.poseStack();

                        // 用 baseSeed + index 派生每节独立但稳定的随机偏移。
                        Random rng = new Random(baseSeed + index * 0x9E3779B97F4A7C15L);

                        // 左右随机错开，幅度较大，不整齐
                        float ox = (rng.nextFloat() - 0.5f) * 0.30f;      // ±0.15
                        // 上下轻微浮动
                        float oy = -0.20f + (rng.nextFloat() - 0.5f) * 0.08f;
                        // 前后轻微浮动
                        float oz = -0.10f + (rng.nextFloat() - 0.5f) * 0.06f;

                        // 随机歪头，制造“每张脸都不同朝向”的观感
                        float yaw   = (rng.nextFloat() - 0.5f) * 30f;      // ±15°
                        float pitch = (rng.nextFloat() - 0.5f) * 20f;      // ±10°
                        float roll  = (rng.nextFloat() - 0.5f) * 15f;      // ±7.5°

                        // 每节轻微缩放差异，进一步破除整齐感
                        float s = 0.80f + (rng.nextFloat() - 0.5f) * 0.10f; // 0.75 ~ 0.85

                        Identifier texture = resolve(skinSource);

                        pose.pushPose();
                        try {
                            pose.translate(ox, oy, oz);
                            // 注意顺序：先旋转再缩放，否则旋转角度会被缩放拉扯
                            pose.mulPose(Axis.YP.rotationDegrees(yaw));
                            pose.mulPose(Axis.XP.rotationDegrees(pitch));
                            pose.mulPose(Axis.ZP.rotationDegrees(roll));
                            pose.scale(s, -s, s); // Y 取负，修正 Minecraft 头模型的朝向

                            collector.submitModelPart(
                                    head,
                                    pose,
                                    RenderTypes.entityCutout(texture),
                                    pass.renderState().lightCoords,
                                    OverlayTexture.NO_OVERLAY,
                                    null
                            );
                        } finally {
                            pose.popPose(); // 无论成功失败都出栈，避免姿态栈泄漏
                        }
                    })
            );
        }
    }

    private static Identifier resolve(String source) {
        if (source == null || source.isEmpty()) return CORPSE_SKIN;

        if (source.startsWith("p:")) {
            try {
                UUID uuid = UUID.fromString(source.substring(2));
                Identifier texture = ClientSkinCache.resolve(uuid).body().texturePath();
                if (REQUESTED.add(source)) {
                    Minecraft.getInstance().getSkinManager()
                            .get(new GameProfile(uuid, ""))
                            .thenAccept(result -> result.ifPresent(skin -> ClientSkinCache.put(uuid, skin)));
                }
                return texture != null ? texture : CORPSE_SKIN;
            } catch (IllegalArgumentException ignored) {
                return CORPSE_SKIN;
            }
        }

        if (source.startsWith("n:")) {
            String name = source.substring(2);
            Identifier cached = ZbSkinCache.get(name);
            if (cached != null) return cached;
            if (REQUESTED.add(source)) {
                ZbSkinIntegration.getPlayerSkinAsync(name)
                        .thenAccept(texture -> { if (texture != null) ZbSkinCache.put(name, texture); });
            }
            return CORPSE_SKIN;
        }

        return CORPSE_SKIN;
    }
}