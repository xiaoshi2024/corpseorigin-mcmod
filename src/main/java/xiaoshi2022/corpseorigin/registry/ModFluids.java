package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.PushReaction;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.InfectedWaterBlock;
import xiaoshi2022.corpseorigin.fluid.InfectedWaterFluid;

public final class ModFluids {

    // ==================== 流体 ====================
    public static final FlowingFluid INFECTED_WATER = Registry.register(
            BuiltInRegistries.FLUID,
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infected_water"),
            new InfectedWaterFluid.Source()
    );

    public static final FlowingFluid FLOWING_INFECTED_WATER = Registry.register(
            BuiltInRegistries.FLUID,
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "flowing_infected_water"),
            new InfectedWaterFluid.Flowing()
    );

    // ==================== 方块 ====================
    public static final Block INFECTED_WATER_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infected_water_block"),
            new InfectedWaterBlock(
                    INFECTED_WATER,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                            .noLootTable()
                            .replaceable()
                            .liquid()
                            .pushReaction(PushReaction.DESTROY)
                            .setId(blockKey("infected_water_block"))  // ✅ 必须设置 ID！
            )
    );

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(BuiltInRegistries.BLOCK.key(), CorpseOrigin.id(path));
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin fluids registered");
    }
}