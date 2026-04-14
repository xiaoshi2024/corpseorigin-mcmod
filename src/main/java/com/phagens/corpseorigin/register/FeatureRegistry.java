package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.worldgen.AlienatedFragmentFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FeatureRegistry {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CorpseOrigin.MODID);

    public static final DeferredHolder<Feature<?>, AlienatedFragmentFeature> ALIENATED_FRAGMENT_FEATURE =
            FEATURES.register("alienated_fragment_feature", AlienatedFragmentFeature::new);
}
