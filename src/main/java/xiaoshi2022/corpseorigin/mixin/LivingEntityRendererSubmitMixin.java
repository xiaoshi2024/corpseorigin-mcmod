package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.CorpsePlayerGeoRenderer;

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
 * 在 HEAD 就先画 Geolib 模型（而不是等原版跑完再画），是为了让它在提交顺序上排在
 * 那些渲染层之前 —— 红眼之类的半透明层才不会反过来被身体盖住。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSubmitMixin {

    @Inject(method = "submit", at = @At("HEAD"))
    private void corpseorigin$submitLimbModel(LivingEntityRenderState state, PoseStack poseStack,
                                              SubmitNodeCollector collector, CameraRenderState camera,
                                              CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        if (!((Object) this instanceof AvatarRenderer<?>)) {
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

        renderer.submit(avatarState, poseStack, collector, camera);
    }
}
