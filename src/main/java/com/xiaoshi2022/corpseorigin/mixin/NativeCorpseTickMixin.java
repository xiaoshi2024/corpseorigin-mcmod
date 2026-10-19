package com.xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;

/**
 * LivingEntity.baseTick() TAIL 注入：NativeCorpseState.tick 处理 incubation 倒计时 / 感染器官 tick。
 */
@Mixin(LivingEntity.class)
public abstract class NativeCorpseTickMixin {
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void corpseorigin$tickIncubation(CallbackInfo ci) {
        if ((Object) this instanceof Mob mob) NativeCorpseState.tick(mob);
    }
}
