package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;

/**
 * 断肢形态下屏蔽原版玩家模型的<b>本体</b>，只让 RenderLayer 继续跑。
 * <p>
 * 必须注入在 {@code setupAnim} 的 TAIL：这个方法每帧都会把各部位的 {@code visible}
 * 按 {@code state} 重设一遍（潜行/旁观之类的逻辑），只有最后覆盖才留得住。
 * <p>
 * 只改 {@code visible} 不改 pose —— RenderLayer（盔甲、披风、外骨骼红眼、其它模组的层）
 * 读的是部位的旋转/位移，不看 visible，所以它们的位置完全不受影响。
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelLimbMixin {

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL")
    )
    private void corpseorigin$hideVanillaBody(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        Integer mask = state.getGeckolibData(LimbRenderData.LIMB_MASK);

        if (mask == null || mask == 0) {
            // ⚠️ 必须自己恢复 head.visible：PlayerModel.setupAnim 只重置 body / 四肢 /
            // 袖子裤子夹克这几个（this.body.visible = showBody 之类），**唯独不碰 head**。
            // 不在这里恢复的话，断肢长好之后头会永久消失。
            self.head.visible = true;
            return;
        }

        self.head.visible = false;
        self.hat.visible = false;
        self.body.visible = false;
        self.jacket.visible = false;
        self.rightArm.visible = false;
        self.rightSleeve.visible = false;
        self.leftArm.visible = false;
        self.leftSleeve.visible = false;
        self.rightLeg.visible = false;
        self.rightPants.visible = false;
        self.leftLeg.visible = false;
        self.leftPants.visible = false;
    }
}
