package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.effect.BYeffect;

public final class ModEffects {

    // ✅ 使用 Holder<MobEffect>
    public static final Holder<MobEffect> QIANS = register(
            "by",
            new BYeffect(MobEffectCategory.HARMFUL, 0xffffff)
    );

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                CorpseOrigin.id(name),
                effect
        );
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin effects registered");
    }
}