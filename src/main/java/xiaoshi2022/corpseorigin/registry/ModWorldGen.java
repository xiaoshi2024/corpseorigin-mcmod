package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 尸水的地形生成。
 * <p>
 * 尸水原本<b>只能由龙右的技能"污染"普通水得到</b>，非龙右玩家在生存里一辈子见不到 ——
 * 桶装、瓶装、感染、克隆仓培养液全都要它。这里补一个地表小血池：
 * <ol>
 *   <li>feature 本体在 {@code data/corpseorigin/worldgen/configured_feature/bywater_lake.json}
 *       （抄的原版 {@code minecraft:lake}，只把流体换成 {@code corpseorigin:infected_water_block}）；
 *   <li>摆放规则在 {@code .../placed_feature/bywater_lake.json}：{@code rarity_filter chance=100}
 *       ≈ 每 100 个区块一块，想更常见就调小这个数；
 *   <li>这里把它挂到主世界所有群系的 {@code LAKES} 生成阶段。
 * </ol>
 */
public final class ModWorldGen {

    /** 地表尸水泉（小池子） */
    public static final ResourceKey<PlacedFeature> BYWATER_LAKE = ResourceKey.create(
            Registries.PLACED_FEATURE, CorpseOrigin.id("bywater_lake"));

    private ModWorldGen() {
    }

    public static void register() {
        BiomeModifications.addFeature(
                context -> context.hasTag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.LAKES,
                BYWATER_LAKE);

        CorpseOrigin.LOGGER.info("CorpseOrigin worldgen registered");
    }
}
