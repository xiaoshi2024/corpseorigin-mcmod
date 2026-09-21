package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;
import xiaoshi2022.corpseorigin.registry.ModItems;

@Mixin(Mob.class)
public class MobPickupMixin {

    /** 扫描半径：只有这个范围内有宝莲灯掉落物，才放开总开关 */
    private static final double LAMP_SCAN_RADIUS = 8.0;

    /**
     * 总开关：默认走原版（僵尸 false、村民 true……）。
     * <p>
     * 只在<b>附近有宝莲灯掉落物</b>时临时放开，让原本不捡东西的生物也能去捡灯。
     * 不能无条件返回 true —— 那会把所有生物的 {@code wantsToPickUp} 全部激活，
     * 僵尸、骷髅会顺带开始捡盔甲和武器。
     */
    @Inject(method = "canPickUpLoot", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$canPickUpLoot(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (!self.isAlive() || self.isBaby()) {
            return;
        }
        if (!corpseorigin$lampNearby(self)) {
            return;   // 附近没灯 → 走原版
        }
        cir.setReturnValue(true);
    }

    /**
     * “想不想捡这一件”。
     * <p>
     * 只放行宝莲灯，其余一律 false —— 否则总开关被上面的灯打开后，
     * 这一带的其他掉落物（盔甲、武器）也会被一起捡走。
     */
    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$wantsToPickUp(ServerLevel level, ItemStack itemStack,
                                            CallbackInfoReturnable<Boolean> cir) {
        // ❌ 克隆仓物品 → 不捡
        if (itemStack.is(ModItems.CLONE_CHAMBER)) {
            cir.setReturnValue(false);
            return;
        }

        // ✅ 宝莲灯 → 捡
        if (itemStack.getItem() instanceof BloodLotusLamp) {
            cir.setReturnValue(true);
            return;
        }

        // ❌ 其他物品 → 一律不捡（总开关可能正被附近的灯打开，不能走原版）
        cir.setReturnValue(false);
    }

    /** 附近（8 格内）有没有宝莲灯掉落物 */
    private static boolean corpseorigin$lampNearby(Mob self) {
        return !self.level().getEntitiesOfClass(
                ItemEntity.class,
                self.getBoundingBox().inflate(LAMP_SCAN_RADIUS),
                e -> !e.isRemoved() && e.getItem().getItem() instanceof BloodLotusLamp
        ).isEmpty();
    }
}