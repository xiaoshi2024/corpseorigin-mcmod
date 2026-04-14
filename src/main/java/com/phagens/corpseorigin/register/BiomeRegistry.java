package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BiomeRegistry {
    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(Registries.BIOME, CorpseOrigin.MODID);

    public static final ResourceKey<Biome> DEAD_SILENCE = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "dead_silence")
    );

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        spawnBuilder.addSpawn(
                net.minecraft.world.entity.MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(
                        EntityRegistry.LOWER_LEVEL_ZB.get(),
                        30, 1, 4
                )
        );
        spawnBuilder.addSpawn(
                net.minecraft.world.entity.MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(
                        EntityRegistry.ZBR_FISH.get(),
                        15, 1, 3
                )
        );
        spawnBuilder.addSpawn(
                net.minecraft.world.entity.MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(
                        EntityRegistry.GUIGUN.get(),
                        5, 1, 2
                )
        );

        BiomeGenerationSettings.Builder genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(genBuilder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(genBuilder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(genBuilder);
        BiomeDefaultFeatures.addDefaultSprings(genBuilder);
        BiomeDefaultFeatures.addSurfaceFreezing(genBuilder);

        context.register(DEAD_SILENCE, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x5A5A5A)
                        .waterColor(0x3D2A1E)
                        .waterFogColor(0x2A1E14)
                        .skyColor(0x6B6B6B)
                        .foliageColorOverride(0x4A3A2A)
                        .grassColorOverride(0x4A3A2A)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(genBuilder.build())
                .build());
    }
}
