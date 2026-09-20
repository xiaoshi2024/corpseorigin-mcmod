package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;

/**
 * 尸巢之子第二形态的<b>独立碰撞箱</b>。
 * <p>
 * 二阶段不用 {@code Attributes.SCALE} 撑体型 —— 那个属性是个"等比放大"，会连宽度
 * （0.6 → 10 格，变成一座塔）、眼高、相机和渲染倍率一起改掉，所以才需要在渲染那边再补一套
 * 抵消逻辑。这里改成把箱子的尺寸整个接管：
 * <ul>
 *   <li>宽 × 高都按模型的视觉尺寸换算（{@code ShiChaoZhiZi.SECOND_FORM_HITBOX_WIDTH / _HEIGHT}，
 *       宽只量躯干、不算尾巴和伸出的手臂）；</li>
 *   <li>眼高改写成模型头部的高度（{@code ShiChaoZhiZi.SECOND_FORM_EYE_HEIGHT}，
 *       同一套倍率换算）—— 相机、准星原点、视线判定都跟着上到"头"上，
 *       不会像以前那样贴着脚底看；</li>
 *   <li>注入点在 {@code LivingEntity#getDimensions}（它是 {@code final}，玩家这一系没有重写，
 *       所以就是玩家实际走的那份实现），{@code refreshDimensions()} 与 {@code getEyeHeight(Pose)}
 *       读的都是它 —— 进出形态时调用一次 {@code refreshDimensions()} 便会立刻生效
 *       （见 {@code ShiChaoZhiZi} 的 apply / remove）。</li>
 * </ul>
 * ⚠️ 客户端那半边的形态号来自网络包缓存：服务端广播变种后，{@code CorpseOriginClient}
 * 会顺手在本地玩家身上补一次 {@code refreshDimensions()}，否则客户端箱子还停在 1.8 格，
 * 跟服务端打架（顶头、卡位置）。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerDimensionsMixin {

    @Inject(
            method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void corpseorigin$shichaoSecondFormHitbox(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof Player self) || !ShiChaoZhiZi.isSecondForm(self)) {
            return;
        }
        cir.setReturnValue(EntityDimensions
                .scalable(ShiChaoZhiZi.SECOND_FORM_HITBOX_WIDTH, ShiChaoZhiZi.SECOND_FORM_HITBOX_HEIGHT)
                .withEyeHeight(ShiChaoZhiZi.SECOND_FORM_EYE_HEIGHT));
    }
}
