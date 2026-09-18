package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.renderer.player.CorpsePlayerGeoRenderer;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 把 GeckoLib 盔甲管线覆盖掉的玩家动画数据补回去 —— 断肢形态下"穿了 geo 套就一根骨骼都不动"的根因。
 * <p>
 * GeckoLib 在 {@code EntityRendererMixin} 里包了原版 {@code extractRenderState}，对每个穿戴 geo 盔甲的槽位
 * 调 {@code captureRenderStates} 准备 per-slot 渲染状态。它挑 per-slot state 的方式是：
 * <pre>
 *   if (slot == EquipmentSlot.HEAD) return 玩家本人的 render state;   // ← 头盔
 *   return 重新 extract 出来的一份全新 render state;                  // ← 胸 / 腿 / 靴
 * </pre>
 * 也就是说<b>只有头盔那一件</b>会借用玩家本人那份 state，随后 {@code fillRenderState} 会把
 * <b>头盔物品</b>的动画控制器快照写进去，把玩家自己的 {@code DataTickets.ANIMATION_CONTROLLER_STATES}
 * 整个盖掉。而渲染时 GeckoLib 的 {@code applyAnimationControllers} 只读这个数组 ——
 * 结果就是玩家身上所有动画停在 0 帧：血管 scale 不生长、残桩不动，
 * 看起来就是"动画没播放"，而摘掉头盔（例如断头时 {@code dropHeadArmor} 把头盔收回背包）立刻恢复。
 * <p>
 * 胸甲 / 护腿因为是各自独立的新 state，不会碰到玩家那份，所以"只穿胸甲没事、带头盔就坏"。
 * <p>
 * 这个注入点排在 {@code captureRenderStates} 的 RETURN，即 GeckoLib 全部盖完之后，把断肢数据重新写回
 * （见 {@link CorpsePlayerGeoRenderer#writeLimbRenderData}）。
 */
@Mixin(GeoArmorRenderer.class)
public abstract class GeoArmorRendererCaptureMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "captureRenderStates", at = @At("RETURN"))
    private static void corpseorigin$restoreLimbRenderData(HumanoidRenderState rootState, LivingEntity entity,
                                                          float partialTick, BiFunction baseModelFn,
                                                          Function stateFn, CallbackInfo ci) {
        if (!(rootState instanceof AvatarRenderState avatarState)) {
            return;
        }
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;   // 只有玩家（含克隆分身）才有断肢这回事
        }
        CorpsePlayerGeoRenderer.writeLimbRenderData(avatarState, player, partialTick);
    }
}
