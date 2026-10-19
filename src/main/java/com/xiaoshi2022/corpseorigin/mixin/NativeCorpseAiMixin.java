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
 * 感染后的 Mob AI 处理：
 * <ul>
 *   <li>友好生物（动物/村民/水生/蝙蝠）：HEAD 取消 {@code serverAiStep}，
 *       整只换成尸兄猎手 AI；</li>
 *   <li>带机制的生物（Boss/怪物/模组生物）：让原版 {@code serverAiStep} 正常跑，
 *       自己的 Goal 与 {@code customServerAiStep} 技能全部保留，
 *       只在 TAIL 叠加一层"无目标时锁定猎物"的尸兄行为。</li>
 * </ul>
 * 必须注入 {@code Mob#serverAiStep} 而不是 {@code aiStep}：
 * {@code Mob.aiStep} 的 HEAD 取消会连同 {@code LivingEntity.aiStep} 里的
 * 重力/移动（travel）一起跳过，导致感染体站在原地不动。
 */
@Mixin(Mob.class)
public abstract class NativeCorpseAiMixin {

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$huntAsCorpse(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (NativeCorpseState.infected(mob) && mob.level() instanceof ServerLevel level
                && NativeCorpseAi.shouldReplaceAi(mob)) {
            NativeCorpseAi.tick(mob, level);
            ci.cancel();
        }
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void corpseorigin$corpseOverlay(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (NativeCorpseState.infected(mob) && mob.level() instanceof ServerLevel level
                && !NativeCorpseAi.shouldReplaceAi(mob)) {
            NativeCorpseAi.overlay(mob, level);
        }
    }
}
