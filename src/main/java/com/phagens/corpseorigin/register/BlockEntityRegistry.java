package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.block.entity.QiXingGuanBlockEntity;
import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import com.phagens.corpseorigin.block.entity.ZBRFleshBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final Supplier<BlockEntityType<QiXingGuanBlockEntity>> QI_XING_GUANS = BLOCK_ENTITIES.register("qi_xing_guan",
            () -> new BlockEntityType<>(QiXingGuanBlockEntity::new, Set.of(BlockRegistry.QI_XING_GUAN.get()),null));

    public static final Supplier<BlockEntityType<ZBRFleshBlockEntity>> ZBR_FLESH = BLOCK_ENTITIES.register("zbr_flesh",
            () -> new BlockEntityType<>(ZBRFleshBlockEntity::new, Set.of(BlockRegistry.ZBR_FLESH.get()),null));
    public static final Supplier<BlockEntityType<TechniqueSwapTableEntity>> TECHNIQUE_SWAP_TABLE = BLOCK_ENTITIES.register("technique_swap_table",
            () -> new BlockEntityType<>(TechniqueSwapTableEntity::new, Set.of(BlockRegistry.TECHNIQUE_SWAP_TABLE.get()), null));
}
