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

    /**
     * 获取死寂群系的ResourceKey
     * 用于在其他地方引用该群系
     * 
     * @return 死寂群系的ResourceKey
     */
    public static ResourceKey<Biome> getDeadSilenceKey() {
        return DEAD_SILENCE;
    }

    /**
     * 配置死寂群系
     * 
     * 【设计意图】
     * 该群系仅用于视觉渲染效果（水色、草色、雾色、天色等），
     * 不改变原群系的地形结构、天气、生物生成规则。
     * 
     * 【注意事项】
     * - 移除了所有carvers（地形雕刻）和features（生成特征）
     * - 移除了所有生物生成配置（monster spawners）
     * - 温度设为0.5（适中，避免积雪或沙漠化）
     * - 降水量设为0（无降水）
     * 
     * 【后续扩展】
     * 如果需要在死寂群系中添加自定义尸兄生成，
     * 请使用 NeoForge 的 SpawnDataProvider 或 MobSpawnEvents，
     * 不要直接在这里添加 spawnBuilder，因为：
     * 1. 群系替换后会继承这里的生成规则，覆盖原群系的生物生成
     * 2. 推荐在事件处理器中根据玩家所在群系动态调整生成
     * 
     * 推荐位置：
     * - com.phagens.corpseorigin.event 包下创建新的 SpawnEventHandler
     * - 使用 LivingSpawnEvent 或 MobSpawnEvents 来控制尸兄生成
     */
    public static void bootstrap(BootstrapContext<Biome> context) {
        // 【重要】空的生物生成配置，不覆盖原群系的生物生成规则
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        // 如果后续需要添加自定义尸兄生成，请使用事件机制，不要在这里添加！

        // 【重要】空的地形生成配置，不改变原群系的结构和地形
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        BiomeGenerationSettings.Builder genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        // 不调用任何 BiomeDefaultFeatures.add...() 方法，保持地形不变

        context.register(DEAD_SILENCE, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
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