package com.xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import com.xiaoshi2022.corpseorigin.registry.ModFluids;

/**
 * 尸水方块（1.20.1 Forge 版本）。
 * <p>
 * 与 26.2 Fabric 版本差异：entityInside 签名变化，无 InsideBlockEffectApplier 参数。
 * 对原版 Mob：通过 {@link com.xiaoshi2022.corpseorigin.growth.CorpseInfection#touchInfectedWater} 触发概率感染。
 * 对村民：直接追加 QIANS 效果（保底感染渠道）。
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

        // 通用感染概率入口
        com.xiaoshi2022.corpseorigin.growth.CorpseInfection.touchInfectedWater(living);

        // 村民保底感染
        if (living instanceof Villager villager) {
            com.xiaoshi2022.corpseorigin.effect.BYeffect.applyInfection(villager, (ServerLevel) level, 200, null);
        }
    }
}
