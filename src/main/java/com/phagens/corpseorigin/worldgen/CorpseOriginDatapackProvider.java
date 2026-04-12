package com.phagens.corpseorigin.worldgen;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.register.BiomeRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CorpseOriginDatapackProvider extends net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.BIOME, BiomeRegistry::bootstrap);

    public CorpseOriginDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(CorpseOrigin.MODID));
    }

    public static void onGatherData(net.neoforged.neoforge.data.event.GatherDataEvent event) {
        event.getGenerator().addProvider(true,
                new CorpseOriginDatapackProvider(
                        event.getGenerator().getPackOutput(),
                        event.getLookupProvider()
                )
        );
    }
}
