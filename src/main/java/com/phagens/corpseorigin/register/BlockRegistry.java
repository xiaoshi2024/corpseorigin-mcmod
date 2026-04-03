package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.block.custom.QiXingGuan;
import com.phagens.corpseorigin.block.custom.ZBRFleshBlock;
import com.phagens.corpseorigin.CorpseOrigin;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
    public static final DeferredRegister.Blocks Blocks = DeferredRegister.createBlocks(CorpseOrigin.MODID);

    // 使用Supplier延迟获取实体类型，避免注册顺序问题
    public static final DeferredBlock<QiXingGuan> QI_XING_GUAN = Blocks.register("qi_xing_guan",
            () -> new QiXingGuan(() -> EntityRegistry.LONGYOU.get())); // 召唤尸王龙右

    // 尸兄肉块 - 尸巢的建筑方块
    public static final DeferredBlock<ZBRFleshBlock> ZBR_FLESH = Blocks.register("zbr_flesh",
            ZBRFleshBlock::new);
}
