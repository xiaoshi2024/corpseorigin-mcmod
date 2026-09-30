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

    /** 黄色强化剂的副作用：等级越高越危险，蓝色中和剂可下调 */
    public static final Holder<MobEffect> SIDE_EFFECT = register(
            "side_effect",
            new xiaoshi2022.corpseorigin.effect.SideEffect(MobEffectCategory.HARMFUL, 0xFFAA00)
    );

    /** 尸兄撕咬流血：每 2 秒 1 颗心，期间无法进食（{@code BleedBlockFoodMixin}）—— 仿 Sans KR */
    public static final Holder<MobEffect> BLEED = register(
            "bleed",
            new xiaoshi2022.corpseorigin.effect.BleedEffect(MobEffectCategory.HARMFUL, 0x8B0000)
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