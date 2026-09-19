package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.component.MutantForm;

/**
 * 左护法变异体形态下，玩家本体<b>不再作为射线可选中的目标</b> —— 挨打改由蛟龙的那几节碰撞箱承担
 * （见 {@code GuardianPartEntity}）。
 * <p>
 * 为什么必须让开本体：变异体形态下比本体大得多的蛇身才该是"看得见的身体"，
 * 而本体的箱子还留在玩家脚底（原版 AABB 不能挪，挪了会连带把移动碰撞一起改坏）。
 * 让本体不被选中之后，近战 / 远程只会命中节，再由节把伤害转给本体。
 * <p>
 * 配套：{@code MutantHitboxHandler} 负责生成那些节；关掉 {@code mutantBody.hitboxes.enabled}
 * 时这里也一并恢复（见 {@link MutantForm#isMutantWithHitboxes}），不会出现"打不到人"的死角。
 */
@Mixin(Player.class)
public abstract class PlayerPickableMixin {

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$mutantNotPickable(CallbackInfoReturnable<Boolean> cir) {
        if (MutantForm.isMutantWithHitboxes((Player) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
