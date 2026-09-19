package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.entity.OwnerBound;

/**
 * 左护法的"身体"（蛟龙的节碰撞箱、脱离出来的尸蛟龙）：<b>主人自己射出的箭要穿过去</b>。
 * <p>
 * 这些东西都可被射线/投射物命中（别人得打得到），但它们往往就挡在主人身前 ——
 * 不拦一下的话，一箭出去就贴在自己身体上，等于完全没法远程。
 * 近战那边不用管：客户端拾取已经跳过"自己身体的箱子"（见两个实体的 {@code isPickable}）。
 * <p>
 * 注入点选 {@code Projectile.canHitEntity}：原版箭（{@code AbstractArrow}）等子类都是
 * {@code super.canHitEntity(...)} 委托回这里，所以这一处就能覆盖绝大多数投射物。
 */
@Mixin(Projectile.class)
public abstract class ProjectileOwnBodyMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$skipOwnBody(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof OwnerBound body
                && body.isOwnedBy(((Projectile) (Object) this).getOwner())) {
            cir.setReturnValue(false);
        }
    }
}
