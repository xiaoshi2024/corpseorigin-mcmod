package com.phagens.corpseorigin.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 异化碎块方块
 * 硬度极低的装饰性方块
 */
public class AlienatedFragmentBlock extends Block {
    
    public AlienatedFragmentBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.1f, 0.5f)
                .sound(SoundType.STONE)
                .mapColor(MapColor.COLOR_BROWN)
        );
    }
}
