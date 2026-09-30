package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.registry.ModEffects;

/**
 * 流血禁食 mixin —— 仿 Sans KR 的"无法靠吃东西回血"机制。
 * <p>
 * 被尔兽王尸兄的獠牙撕咬后挂上 {@link ModEffects#BLEED} 效果，
 * 期间玩家任何<b>可食用</b>物品（食物 / 药水）的 {@link Consumable#canConsume} 一律拦回 false，
 * 并显示"獠牙撕咬让你无法进食"消息，让"被咬一口"变成持续威胁。
 * <p>
 * <b>优先级 = 1500</b>：必须在 {@link ItemConsumeMixin}（默认 1000）之后跑。
 * 后者会在 RETURN 把"原应禁食"重置为"允许生肉"以让气血能补；本 mixin 在它之后覆盖回 false，
 * 确保流血期间连生肉都吃不下。
 */
@Mixin(value = Consumable.class, priority = 1500)
public class BleedBlockFoodMixin {

    @Inject(method = "canConsume", at = @At("RETURN"), cancellable = true)
    private void corpseorigin$blockFoodWhileBleeding(LivingEntity entity, ItemStack stack,
                                                     CallbackInfoReturnable<Boolean> cir) {
        // 已允许吃且 entity 有 BLEED 效果 → 拦下：必须靠位移 / 队友 / 等待流血结束才能恢复。
        // 注意：canConsume 是 Consumable 组件实例方法，能被调到说明 stack 必有 Consumable，
        //      无需再额外检查 isEdible（26.2 已无此 API）。
        if (cir.getReturnValue() && entity instanceof ServerPlayer player
                && player.hasEffect(ModEffects.BLEED)) {
            cir.setReturnValue(false);
            player.sendOverlayMessage(
                    Component.translatable("message.corpseorigin.bleed.no_eat"));
        }
    }
}
