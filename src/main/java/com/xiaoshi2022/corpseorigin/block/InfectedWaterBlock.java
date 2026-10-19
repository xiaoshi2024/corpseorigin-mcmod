package com.xiaoshi2022.corpseorigin.block;

import com.xiaoshi2022.corpseorigin.growth.CorpseInfection;
import com.xiaoshi2022.corpseorigin.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * 尸水方块（1.20.1 Forge 版本）。
 * <p>
 * 与 26.2 Fabric 版本差异：entityInside 签名变化，无 InsideBlockEffectApplier 参数。
 * 每次接触都交给 {@link CorpseInfection#touchInfectedWater} 累计浸泡时间：
 * 鱼类立即感染，其它生物连续浸泡 15 秒后感染（数值见服务端配置）。
 */
public class InfectedWaterBlock extends LiquidBlock {

    public InfectedWaterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;

        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() != ModFluids.INFECTED_WATER.get()
                && fluidState.getType() != ModFluids.FLOWING_INFECTED_WATER.get()) return;

        CorpseInfection.touchInfectedWater(living);
    }
}
