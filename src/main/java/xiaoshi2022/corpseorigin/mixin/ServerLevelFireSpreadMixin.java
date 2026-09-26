package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.skill.chapter.FlameSea;

/**
 * 「烈焰火海」铺下的火不许蔓延。
 * <p>
 * {@code FireBlock.tick} 的第一句就是这个判定：
 * {@code if (!level.canSpreadFireAround(pos)) return;} ——
 * 所以只要对火海登记过的位置返回 {@code false}，那处火就不会点燃邻居。
 * 只拦这些位置：世界上其他火焰（雨、gamerule 那套）原样生效。
 */
@Mixin(ServerLevel.class)
public class ServerLevelFireSpreadMixin {

    @Inject(method = "canSpreadFireAround", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$flameSeaDoesNotSpread(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (FlameSea.isProtected((ServerLevel) (Object) this, pos)) {
            cir.setReturnValue(false);
        }
    }
}
