package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.CorpsePlayerGeoRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.MutantBodyRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiLinkRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiLinkRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.ZuoGuardianBodyRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.ShiChaoBodyRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.ShiChaoZhiZiBodyRenderer;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;
import xiaoshi2022.corpseorigin.character.ZuoHuFa;

/**
 * 断肢形态下用 Geolib 模型替换玩家"身体"。
 * <p>
 * 26.2 的 {@code submit} 声明在 LivingEntityRenderer 上（AvatarRenderer 没有重写），
 * 所以注入点选在这里，用 instanceof 把范围收窄到玩家身上。
 * <p>
 * <b>关键：不 cancel 原版 submit</b>。原版 submit 会依次做三件事 ——
 * 画模型本体、跑所有 RenderLayer、画名字；我们只把"模型本体"换掉，
 * 剩下两步照旧，这样盔甲 / 披风 / 外骨骼 / 其它模组的自定义层都能继续正常渲染。
 * 原版模型本体由 {@link PlayerModelLimbMixin} 把各部位 {@code visible=false} 屏蔽掉，
 * 而 RenderLayer 只读部位的 pose、不看 visible，所以不受影响。
 * <p>
 * 在 TAIL 画（原版模型本体 + 所有 RenderLayer 都提交完之后）而不是 HEAD，是为了让
 * 残桩 / 血管在提交顺序上排在盔甲层之后 —— 它们是"穿模显示"的那一层，必须画在长袍、护腿之上
 * （配合 {@link CorpsePlayerGeoRenderer} 里的 ZOffset RenderType 才能真的盖住盔甲；
 * 顺序在前的话，后画的盔甲照样会把它盖回去）。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSubmitMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$submitShiChaoBody(LivingEntityRenderState state, PoseStack poseStack,
                                                SubmitNodeCollector collector, CameraRenderState camera,
                                                CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)
                || !((Object) this instanceof AvatarRenderer<?>)) return;
        if (!Boolean.TRUE.equals(avatarState.getGeckolibData(ShiChaoBodyRenderData.ACTIVE))) return;
        ShiChaoZhiZiBodyRenderer renderer = ShiChaoZhiZiBodyRenderer.get();
        if (renderer == null) return;

        poseStack.pushPose();
        // 资源模型本体就有 646 个模型像素 ≈ 40 格高，所以这里必须整体缩一次。
        // 倍率写在 ShiChaoZhiZi.SECOND_FORM_MODEL_SCALE（唯一开关），视高也跟着它换算。
        float scale = avatarState.scale * ShiChaoZhiZi.SECOND_FORM_MODEL_SCALE;
        poseStack.scale(scale, scale, scale);
        renderer.submit(avatarState, poseStack, collector, camera);
        poseStack.popPose();
        ci.cancel();
    }

    /**
     * 尸体模型的垂直校准值（格）。
     * <p>
     * 改成"分肢体替换"之后，geo 里只有残桩/血管会出画，而它们的挂点骨枢轴和原版骨骼是同一套坐标
     * （腿 0→12、臂 12→24 那块逐块对得上），所以零点一致、这里是 0。
     * 万一残桩整体偏高/偏低，只改这一个数即可（负值往下压、正值往上抬）。
     */
    private static final double CORPSE_MODEL_Y_OFFSET = -0.01;

    /**
     * 穿模显示的前移量（<b>模型像素</b>）：残桩 / 血管朝相机方向挪这么多，越过贴身盔甲外壳。
     * <p>
     * 用模型像素而不是"格"来写，是因为这个位移必须跟着 {@link AvatarRenderState#scale} 走：
     * 模型 1 像素 = {@code scale / 16} 格，写死格数的话，尸王原体（scale 0.15）下会被放大成 8 像素以上
     * 的位移（残桩直接飘出身体），正常体型下却只有 1 像素。盔甲外扩层是 0.25 像素，0.75 像素够穿出去。
     */
    private static final double LIMB_OVERLAY_PUSH_PIXELS = 0.75;

    /**
     * 左护法变异体形态：<b>整体替换</b>玩家 —— 原版模型、盔甲、披风、外骨骼等层全部不画，
     * 只提交 zuo_guardian（巨蛇 + 骑手）。
     * <p>
     * 所以在 HEAD 就接管并 {@code cancel}（而不是像断肢那样在 TAIL 补画一层）。
     * 代价是名字牌也一并不画 —— 这是"变成一具变异体"的取舍，见 {@code MutantBodyRenderData}。
     * <p>
     * ⚠️ 原版是在 submit 内部才 scale(state.scale)（体型缩放），我们在 HEAD 就画了，
     * 这一步得自己补，否则缩小状态下的变异体不会跟着缩。缩放/垂直微调见配置
     * {@code mutantBody.scale} / {@code mutantBody.yOffset}（模型是 BOSS 体型，默认缩到一半）。
     */
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$submitMutantBody(LivingEntityRenderState state, PoseStack poseStack,
                                               SubmitNodeCollector collector, CameraRenderState camera,
                                               CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        if (!((Object) this instanceof AvatarRenderer<?>)) {
            return;
        }
        if (avatarState.getGeckolibData(MutantBodyRenderData.BODY_TEXTURE) == null) {
            return;   // 不是变异体形态（或这一帧纹理没合成出来）→ 完全走原版渲染
        }

        ZuoGuardianBodyRenderer renderer = ZuoGuardianBodyRenderer.get();
        if (renderer == null) {
            return;
        }

        CorpseConfig.MutantBody config = CorpseConfig.get().mutantBody;

        poseStack.pushPose();
        poseStack.translate(0.0, config.yOffset, 0.0);
        // 实体 SCALE 负责真实眼高和碰撞箱；抵消一次，维持配置中原有的青龙视觉尺寸。
        float scale = avatarState.scale * (config.scale / ZuoHuFa.MERGED_SCALE);
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
        renderer.submit(avatarState, poseStack, collector, camera);
        poseStack.popPose();

        ci.cancel();
    }

    /**
     * 开胃奶「拦腰斩断」形态：<b>整体替换</b>玩家 —— 原版模型、盔甲、披风、外骨骼等层全部不画，
     * 只提交 {@code niunai_link_player}（那具被斩成两截、带接回绳子的身体）。
     * <p>
     * 唯一的例外是<b>菊花盾背挂</b>：它长在背后、和身体是两套独立骨骼，所以和这具身体同时出画
     * （见下方手动补画的那一层）。
     * <p>
     * 同变异体，在 HEAD 接管并 {@code cancel}。模型骨骼就是原版玩家那套绝对坐标
     * （脚底 0 / 肩 24 / 头顶 32），理论上零点齐平，所以 {@link #NIUNAI_LINK_Y_OFFSET} 是 0，
     * 万一整体偏高/偏低只改这一个数即可。
     */
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$submitNiunaiLinkBody(LivingEntityRenderState state, PoseStack poseStack,
                                                   SubmitNodeCollector collector, CameraRenderState camera,
                                                   CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        if (!((Object) this instanceof AvatarRenderer<?>)) {
            return;
        }
        // ticket 是 AvatarRendererMixin 每帧写的；不是 true = 不在腰斩形态 → 完全走原版渲染
        if (!Boolean.TRUE.equals(avatarState.getGeckolibData(NiunaiLinkRenderData.ACTIVE))) {
            return;
        }
        // 这一帧不画原版模型，所以名字牌之类也一并没了（同变异体那套的取舍）

        NiunaiLinkRenderer renderer = NiunaiLinkRenderer.get();
        if (renderer == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0, NIUNAI_LINK_Y_OFFSET, 0.0);
        // 原版是在 submit 内部才 scale(state.scale)，我们在 HEAD 就画了，这一步得自己补
        float scale = avatarState.scale;
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
        renderer.submit(avatarState, poseStack, collector, camera);

        // ★ 菊花盾背挂和这具被斩成两截的身体同时存在。
        //   因为下面把原版 submit 整个 cancel 了，{@code corpseorigin$submitNiunaiBackMount}
        //   那条 TAIL 注入不会跑 —— 所以在这里手动补画一层（两套模型用的是同一套绝对坐标，变换直接复用）。
        NiunaiXRenderer backMount = NiunaiXRenderer.get();
        if (backMount != null
                && Boolean.TRUE.equals(avatarState.getGeckolibData(NiunaiXRenderData.ACTIVE))) {
            backMount.submit(avatarState, poseStack, collector, camera);
        }

        poseStack.popPose();

        ci.cancel();
    }

    /** 开胃奶「拦腰斩断」模型的垂直校准值（格）；骨骼与原版玩家同一套坐标，所以是 0 */
    private static final double NIUNAI_LINK_Y_OFFSET = 0.0;

    @Inject(method = "submit", at = @At("TAIL"))
    private void corpseorigin$submitLimbModel(LivingEntityRenderState state, PoseStack poseStack,
                                              SubmitNodeCollector collector, CameraRenderState camera,
                                              CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }

        Integer mask = avatarState.getGeckolibData(LimbRenderData.LIMB_MASK);
        if (mask == null || mask == 0) {
            return;   // 四肢完好 → 完全走原版渲染
        }

        CorpsePlayerGeoRenderer renderer = CorpsePlayerGeoRenderer.get();
        if (renderer == null) {
            return;
        }

        // 原版是在 submit 内部才 scale(state.scale)（体型缩放，比如尸王原体的 0.15），
        // 而我们在 submit 的 HEAD 就把 Geo 模型画了 —— 这一步得自己补，
        // 否则缩小状态下的身体不会跟着缩，看起来比盔甲大一整圈。
        poseStack.pushPose();
        // corpse_player 的零点比原版玩家模型高 2px（实测值），补回来才不会浮在盔甲上面。
        // 想在游戏里微调就改这个常量：负值 = 往下压。
        poseStack.translate(0.0, CORPSE_MODEL_Y_OFFSET, 0.0);
        float scale = avatarState.scale;
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
        // ★ 穿模显示：把这层残桩/血管朝"相机方向"整体前移一点点，物理上越过贴身盔甲的表面。
        // 只靠 RenderType 的 ZOffset 不行 —— 那是叠加层用的视觉空间微偏移（万分之一格量级），
        // 压不过盔甲与残桩之间 1 像素左右的真实间距。这里按相机朝向平移，
        // 深度测试照常保留，所以地形/墙体依旧能正常挡住它（不会透视穿墙）。
        //
        // ⚠️ 两个必须守住的点：
        //   ① 位移写在 scale 之后、量用"模型像素"（px/16）——这样它随体型自动缩放，
        //      尸王原体（0.15）和正常体型下的观感一致；
        //   ② 门槛只判 camera == null，<b>不要</b>再判 camera.initialized —— 后者在实体提交阶段
        //      并不保证为 true，一旦为 false 整段前移会被静默跳过，症状正是"裸装看得见、穿盔甲被盖住"。
        //      模组里另一处取相机的代码（AntennaArmFirstPerson）同样只判非空。
        if (camera != null) {
            double push = LIMB_OVERLAY_PUSH_PIXELS / 16.0;
            Vec3 look = Vec3.directionFromRotation(camera.xRot, camera.yRot);
            poseStack.translate(-look.x * push, -look.y * push, -look.z * push);
        }
        renderer.submit(avatarState, poseStack, collector, camera);
        poseStack.popPose();
    }

    /**
     * 开胃奶背挂的垂直校准值（格）。
     * <p>
     * {@code niunaix.geo.json} 与 {@code corpse_player.geo.json} 用的是同一套绝对坐标
     * （脚底 0 / 肩 24 / 头顶 32），理论上零点是齐的，所以这里是 0。
     * 万一背挂整体偏高/偏低，只改这一个数即可（负值往下压、正值往上抬）。
     */
    private static final double NIUNAI_Y_OFFSET = 0.0;

    /**
     * 开胃奶背挂形态下，在玩家背后<b>补画</b>一层 {@code niunaix}。
     * <p>
     * 提交点同断肢（submit 的 TAIL）：原版模型、盔甲、披风、其它模组的层都照常渲染，
     * 我们只在最上面补一层。背挂长在背后，深度测试会让身体正常挡住它，所以不需要像残桩 / 血管
     * 那样朝相机前移。
     * <p>
     * ⚠️ 原版是在 submit 内部才 scale(state.scale)（体型缩放），我们在 TAIL 补画，
     * 这一步得自己补，否则缩小状态下的背挂不会跟着缩。
     */
    @Inject(method = "submit", at = @At("TAIL"))
    private void corpseorigin$submitNiunaiBackMount(LivingEntityRenderState state, PoseStack poseStack,
                                                    SubmitNodeCollector collector, CameraRenderState camera,
                                                    CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        if (!((Object) this instanceof AvatarRenderer<?>)) {
            return;
        }
        // ticket 是 AvatarRendererMixin 每帧写的（不是背挂形态时写 false）；
        // 不是 true = 这一帧不该出背挂 → 完全走原版渲染
        if (!Boolean.TRUE.equals(avatarState.getGeckolibData(NiunaiXRenderData.ACTIVE))) {
            return;
        }

        NiunaiXRenderer renderer = NiunaiXRenderer.get();
        if (renderer == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0, NIUNAI_Y_OFFSET, 0.0);
        float scale = avatarState.scale;
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
        renderer.submit(avatarState, poseStack, collector, camera);
        poseStack.popPose();
    }
}
