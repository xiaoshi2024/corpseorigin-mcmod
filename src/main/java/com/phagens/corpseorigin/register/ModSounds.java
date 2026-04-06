package com.phagens.corpseorigin.register; // 根据你的实际包名调整

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    // 1. 创建 DeferredRegister，用于注册 SoundEvent
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CorpseOrigin.MODID);

    // 2. 注册"吃~~"音效
    public static final Supplier<SoundEvent> GROUND_CHI = registerSoundEvent("ground_chi");
    
    // 死寂群系背景音乐
    public static final Supplier<SoundEvent> DEAD_SILENCE_AMBIENCE = registerSoundEvent("silence");

    // 3. 辅助方法：注册单个音效
    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, name);
        // 创建 SoundEvent，第二个参数是用于引用它的固定 ID
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    // 4. 将此注册表添加到主类的总线上
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}