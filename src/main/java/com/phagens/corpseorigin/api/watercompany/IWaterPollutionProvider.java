package com.phagens.corpseorigin.api.watercompany;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface IWaterPollutionProvider {

    boolean isWaterPolluted(ServerLevel level, BlockPos pos);

    int getPollutionLevel(ServerLevel level, BlockPos pos);

    boolean isBiomePolluted(ServerLevel level, BlockPos pos);

    boolean isWaterExplicitlyInfected(ServerLevel level, BlockPos pos);

    void markWaterAsPolluted(ServerLevel level, BlockPos pos);

    void markWaterAsClean(ServerLevel level, BlockPos pos);
}