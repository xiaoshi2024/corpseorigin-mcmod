package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xiaoshi2022.corpseorigin.growth.RealmProgression;

@Mixin(LivingEntity.class)
public class RealmDamageMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float corpseorigin$realmDamage(float amount, ServerLevel level, DamageSource source, float original) {
        return RealmProgression.adjustDamage((LivingEntity)(Object)this, source, amount);
    }
}
