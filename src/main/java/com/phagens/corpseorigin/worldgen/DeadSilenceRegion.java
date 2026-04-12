package com.phagens.corpseorigin.worldgen;

import com.mojang.datafixers.util.Pair;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.register.BiomeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class DeadSilenceRegion extends Region {

    public DeadSilenceRegion() {
        super(ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "dead_silence_region"), RegionType.OVERWORLD, 2);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addBiomeSimilar(mapper, Biomes.DESERT, BiomeRegistry.DEAD_SILENCE);
    }
}
