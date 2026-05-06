package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.custom.AlienatedFragmentBlock;
import com.phagens.corpseorigin.block.custom.QiXingGuan;
import com.phagens.corpseorigin.block.custom.TechniqueSwapTableBlock;
import com.phagens.corpseorigin.block.custom.ZBRFleshBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
    public static final DeferredRegister.Blocks Blocks = DeferredRegister.createBlocks(CorpseOrigin.MODID);

    public static final DeferredBlock<QiXingGuan> QI_XING_GUAN = Blocks.register("qi_xing_guan",
            () -> new QiXingGuan(() -> EntityRegistry.LONGYOU.get()));

    public static final DeferredBlock<ZBRFleshBlock> ZBR_FLESH = Blocks.register("zbr_flesh",
            ZBRFleshBlock::new);

    public static final DeferredBlock<AlienatedFragmentBlock> ALIENATED_FRAGMENT = Blocks.register("alienated_fragment",
            AlienatedFragmentBlock::new);

    public static final DeferredBlock<TechniqueSwapTableBlock> TECHNIQUE_SWAP_TABLE = Blocks.register("technique_swap_table",
            () -> new TechniqueSwapTableBlock(Block.Properties.of()));
}
