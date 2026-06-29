package com.phagens.corpseorigin.api.watercompany;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public interface ICorpseWaterHandler {

    Fluid getCorpseWaterFluid();

    boolean isCorpseWater(Fluid fluid);

    ItemStack createCorpseWaterBottle();

    ItemStack createCorpseWaterBucket();

    boolean canBeConvertedToCorpseWater(ServerLevel level, BlockPos pos);

    void convertWaterToCorpseWater(ServerLevel level, BlockPos pos);

    void convertCorpseWaterToCleanWater(ServerLevel level, BlockPos pos);

    int getCorpseWaterEnergy(ServerLevel level, BlockPos pos);

    void setCorpseWaterEnergy(ServerLevel level, BlockPos pos, int energy);

    int getMaxCorpseWaterEnergy();

    FluidStack extractFluidFromPosition(ServerLevel level, BlockPos pos, int amount);

    boolean isPositionCorpseWaterSource(ServerLevel level, BlockPos pos);
}