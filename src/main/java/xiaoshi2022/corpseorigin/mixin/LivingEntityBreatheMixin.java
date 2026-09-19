package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.component.MutantForm;

/**
 * 左护法蛟龙形态：<b>水下呼吸</b>。
 * <p>
 * 原著里他就是与青龙合体的尸兄（尸王四大神宠「青龙」），蛟龙属水 ——
 * 所以穿着这具身体泡在水里不该被淹。判定复用 {@link MutantForm#isMutant}（双端都能算），
 * 于是服务端算氧、客户端画气泡条都一致。
 * <p>
 * 为什么注入 {@code LivingEntity} 而不是 {@code Player}：这个方法只有 {@code LivingEntity} 声明，
 * 玩家没有重写它 —— 注入父类再用 {@code instanceof Player} 收窄，是唯一能拦到的位置。
 * <p>
 * 配套：{@code ZuoHuFa} 的合体那一档还把 {@code WATER_MOVEMENT_EFFICIENCY} 拉满（游得快），
 * 动画那边在水里播 {@code swim}。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBreatheMixin {

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$mutantBreathesUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && MutantForm.isMutant(player)) {
            cir.setReturnValue(true);
        }
    }
}
