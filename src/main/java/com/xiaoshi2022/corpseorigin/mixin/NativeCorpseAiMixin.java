package com.xiaoshi2022.corpseorigin.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.xiaoshi2022.corpseorigin.entity.ai.NativeCorpseAi;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;

/**
 * 感染后的 Mob：用尸兄猎手 AI 顶掉原版 goal AI。
 * <p>
 * 必须注入 {@code Mob#serverAiStep} 而不是 {@code aiStep}：
 * {@code Mob.aiStep} 的 HEAD 取消会连同 {@code LivingEntity.aiStep} 里的
 * 重力/移动（travel）一起跳过，导致感染体站在原地不动。
 * {@code serverAiStep} 只包含 goal/导航/感知逻辑，取消它既能屏蔽原版 AI，
 * 又保留物理与移动。
 */
@Mixin(Mob.class)
public abstract class NativeCorpseAiMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$huntAsCorpse(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (NativeCorpseState.infected(mob) && mob.level() instanceof ServerLevel level) {
            NativeCorpseAi.tick(mob, level);
            ci.cancel();
        }
    }
}
