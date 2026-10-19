package com.xiaoshi2022.corpseorigin.registry;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CorpseOrigin.MODID);
    public static final RegistryObject<SoundEvent> GROUND_CHI = register("ground_chi");
    public static final RegistryObject<SoundEvent> CORPSE_BREATH = register("corpse_breath");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(CorpseOrigin.MODID, name)));
    }
}
