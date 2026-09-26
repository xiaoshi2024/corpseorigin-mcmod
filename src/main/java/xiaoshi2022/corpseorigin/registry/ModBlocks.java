package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.CNChessZbrsBlock;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.ZBRFleshBlock;

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

    /** 尸兄肉块：尸巢的基本建筑方块，外观由 GeckoLib 的 BER 渲染（所以 render shape 是 INVISIBLE） */
    public static final Block ZBR_FLESH = register(
            "zbr_flesh",
            new ZBRFleshBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_SAND)
                    .strength(2.0F, 3600000.0F)
                    .mapColor(MapColor.COLOR_RED)
                    .noOcclusion()
                    .randomTicks()
                    .setId(blockKey("zbr_flesh")))
    );

    /** 象棋尸兄（方块实体，GeckoLib 动画） */
    public static final Block CN_CHESS_ZBRS = register(
            "cn_chess_zbrs",
            new CNChessZbrsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                    // 挖掘硬度 -1 = 不可挖掘（同基岩）；爆炸抗性设 0，
                    // 这样爆炸会"选中"方块并回调 wasExploded，由我们折成 HP 伤害，
                    // 而不是直接被炸没
                    .strength(-1.0F, 0.0F)
                    .mapColor(MapColor.COLOR_BLACK)
                    .noOcclusion()
                    .randomTicks()
                    .setId(blockKey("cn_chess_zbrs")))
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
