package com.xiaoshi2022.corpseorigin.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;

/**
 * NativeCorpseState 数据持久化注入：在 LivingEntity 存档/读档时写入/读取
 * appearance、organs、incubation 缓存，保证重启不丢失。
 */
@Mixin(LivingEntity.class)
public abstract class NativeCorpseDataMixin {

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void corpseorigin$writeNativeCorpseData(CompoundTag tag, CallbackInfo ci) {
        NativeCorpseState.writeNbt((LivingEntity) (Object) this, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void corpseorigin$readNativeCorpseData(CompoundTag tag, CallbackInfo ci) {
        NativeCorpseState.readNbt((LivingEntity) (Object) this, tag);
    }
}
