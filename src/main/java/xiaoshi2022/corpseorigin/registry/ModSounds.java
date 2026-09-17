package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 自定义音效注册（Fabric 26.2）。
 * <p>
 * 沿用 1.21.1 模组的音效命名。{@link #GROUND_CHI} 为尸兄通用"吃~~"音效
 * （原作：地阶尸兄进食时发出的"吃~~"声）。
 */
public final class ModSounds {

    /** 尸兄通用"吃~~"音效 */
    public static final SoundEvent GROUND_CHI = register("ground_chi");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = CorpseOrigin.id(name);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin sounds registered");
    }
}
