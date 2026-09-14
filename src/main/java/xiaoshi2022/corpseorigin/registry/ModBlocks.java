package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;

public final class ModBlocks {

    // 直接引用 ModFluids 中的方块
    public static final Block INFECTED_WATER_BLOCK = ModFluids.INFECTED_WATER_BLOCK;

    /** 黑色火线克隆仓（双高，玻璃仓体 + 铁框架，外观由 BER 渲染） */
    public static final Block CLONE_CHAMBER = register(
            "clone_chamber",
            new CloneChamberBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey("clone_chamber")))
    );

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, CorpseOrigin.id(name), block);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(BuiltInRegistries.BLOCK.key(), CorpseOrigin.id(path));
    }

    public static void init() {
        // 静态初始化已经完成
        CorpseOrigin.LOGGER.info("CorpseOrigin blocks registered");
    }
}
