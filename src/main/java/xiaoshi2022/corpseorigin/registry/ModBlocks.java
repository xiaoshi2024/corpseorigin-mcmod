package xiaoshi2022.corpseorigin.registry;

import net.minecraft.world.level.block.Block;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class ModBlocks {

    // 直接引用 ModFluids 中的方块
    public static final Block INFECTED_WATER_BLOCK = ModFluids.INFECTED_WATER_BLOCK;

    public static void init() {
        // 静态初始化已经完成
        CorpseOrigin.LOGGER.info("CorpseOrigin blocks registered");
    }
}