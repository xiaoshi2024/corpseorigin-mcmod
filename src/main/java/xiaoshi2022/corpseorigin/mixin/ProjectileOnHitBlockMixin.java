package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;

/**
 * 弹射物命中象棋尸兄方块时，把命中折成 HP 伤害。
 * <p>
 * 箭系（含三叉戟投掷物）：伤害 = baseDamage × 箭矢速度，与箭矢命中实体的
 * 原版算法一致（AbstractArrow.onHitEntity）。AbstractArrow.onHitBlock 会调用
 * super，所以注入 Projectile.onHitBlock 的 TAIL 对箭也生效。
 */
@Mixin(Projectile.class)
public class ProjectileOnHitBlockMixin {

    @Inject(method = "onHitBlock", at = @At("TAIL"))
    private void corpseorigin$hitChess(BlockHitResult hitResult, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (!(serverLevel.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs)) {
            return;
        }

        if (self instanceof AbstractArrow arrow) {
            double baseDamage = ((AbstractArrowAccessor) arrow).corpseorigin$getBaseDamage();
            float amount = (float) (baseDamage * arrow.getDeltaMovement().length());
            if (amount <= 0.0F) {
                return;
            }
            Entity owner = arrow.getOwner();
            DamageSource source = serverLevel.damageSources()
                    .arrow(arrow, owner != null ? owner : arrow);
            zbrs.hurt(serverLevel, source, amount);
        }
        // 其他弹射物（火球等）以后在此扩展分支
    }
}
