package com.xiaoshi2022.corpseorigin.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import com.xiaoshi2022.corpseorigin.effect.BYeffect;
import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CorpseOrigin.MODID);

    public static final RegistryObject<MobEffect> QIANS = EFFECTS.register("by",
            () -> new BYeffect(MobEffectCategory.HARMFUL, 0xffffff));

    public static void register() {
        // 在主类构造函数里调用 EFFECTS.register(modEventBus) 即可
    }
}
