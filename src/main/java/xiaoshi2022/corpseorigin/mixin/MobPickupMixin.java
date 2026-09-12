package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

@Mixin(Mob.class)
public class MobPickupMixin {

    /** ✅ 让所有生物都「会捡东西」（但不会改变它们「想捡什么」） */
    @Inject(method = "canPickUpLoot", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$canPickUpLoot(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self.isAlive() && !self.isBaby()) {
            cir.setReturnValue(true);
        }
    }

    /** ✅ 宝莲灯 = 额外「想要」，其他物品走原版 */
    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$wantsToPickUp(ServerLevel level, ItemStack itemStack,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.getItem() instanceof BloodLotusLamp) {
            cir.setReturnValue(true);
        }
        // ✅ 不是宝莲灯 → 不 setReturnValue，走原版逻辑
    }
}