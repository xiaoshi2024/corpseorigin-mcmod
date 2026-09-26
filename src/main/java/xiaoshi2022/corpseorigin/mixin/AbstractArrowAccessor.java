package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.2 的箭矢伤害重构：{@code baseDamage} 是 protected、无公开 getter
 * （只有 {@code setBaseDamage}）。暴露给 Projectile mixin 折算方块伤害。
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Accessor("baseDamage")
    double corpseorigin$getBaseDamage();
}
