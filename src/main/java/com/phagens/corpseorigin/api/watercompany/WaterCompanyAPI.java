package com.phagens.corpseorigin.api.watercompany;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.data.InfectionData;
import com.phagens.corpseorigin.effect.BYeffect;
import com.phagens.corpseorigin.register.BiomeRegistry;
import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.UUID;

public final class WaterCompanyAPI {

    private static IWaterPollutionProvider pollutionProvider;
    private static IInfectionTrigger infectionTrigger;
    private static ICorpseWaterHandler corpseWaterHandler;

    private WaterCompanyAPI() {
    }

    public static void init() {
        pollutionProvider = new DefaultPollutionProvider();
        infectionTrigger = new DefaultInfectionTrigger();
        corpseWaterHandler = new DefaultCorpseWaterHandler();
        CorpseOrigin.LOGGER.info("WaterCompanyAPI initialized");
    }

    public static IWaterPollutionProvider getPollutionProvider() {
        return pollutionProvider;
    }

    public static IInfectionTrigger getInfectionTrigger() {
        return infectionTrigger;
    }

    public static ICorpseWaterHandler getCorpseWaterHandler() {
        return corpseWaterHandler;
    }

    public static void setPollutionProvider(IWaterPollutionProvider provider) {
        pollutionProvider = provider;
        CorpseOrigin.LOGGER.info("WaterCompanyAPI: Pollution provider overridden");
    }

    public static void setInfectionTrigger(IInfectionTrigger trigger) {
        infectionTrigger = trigger;
        CorpseOrigin.LOGGER.info("WaterCompanyAPI: Infection trigger overridden");
    }

    public static void setCorpseWaterHandler(ICorpseWaterHandler handler) {
        corpseWaterHandler = handler;
        CorpseOrigin.LOGGER.info("WaterCompanyAPI: Corpse water handler overridden");
    }

    private static class DefaultPollutionProvider implements IWaterPollutionProvider {

        @Override
        public boolean isWaterPolluted(ServerLevel level, BlockPos pos) {
            return isBiomePolluted(level, pos) || isWaterExplicitlyInfected(level, pos);
        }

        @Override
        public int getPollutionLevel(ServerLevel level, BlockPos pos) {
            if (isWaterExplicitlyInfected(level, pos)) {
                return InfectionData.get(level).getWaterEnergy(pos);
            }
            if (isBiomePolluted(level, pos)) {
                return 10;
            }
            return 0;
        }

        @Override
        public boolean isBiomePolluted(ServerLevel level, BlockPos pos) {
            return level.getBiome(pos).is(BiomeRegistry.DEAD_SILENCE);
        }

        @Override
        public boolean isWaterExplicitlyInfected(ServerLevel level, BlockPos pos) {
            return InfectionData.isWaterInfectedStatic(level, pos);
        }

        @Override
        public void markWaterAsPolluted(ServerLevel level, BlockPos pos) {
            InfectionData.markWaterInfectedStatic(level, pos);
        }

        @Override
        public void markWaterAsClean(ServerLevel level, BlockPos pos) {
            InfectionData.get(level).setWaterEnergy(pos, 0);
        }
    }

    private static class DefaultInfectionTrigger implements IInfectionTrigger {

        @Override
        public void triggerInfection(LivingEntity target, ServerLevel level) {
            BYeffect.applyInfection(target, level);
        }

        @Override
        public void triggerInfection(LivingEntity target, ServerLevel level, int durationTicks) {
            BYeffect.applyInfection(target, level, durationTicks);
        }

        @Override
        public void triggerInfection(LivingEntity target, ServerLevel level, UUID sourceUUID) {
            BYeffect.applyInfection(target, level, sourceUUID);
        }

        @Override
        public void triggerInfection(LivingEntity target, ServerLevel level, int durationTicks, UUID sourceUUID) {
            BYeffect.applyInfection(target, level, durationTicks, sourceUUID);
        }

        @Override
        public boolean canInfect(LivingEntity target) {
            return BYeffect.canInfect(target);
        }

        @Override
        public boolean isInfected(LivingEntity target) {
            return target.hasEffect(com.phagens.corpseorigin.register.EffectRegister.QIANS);
        }

        @Override
        public void removeInfection(LivingEntity target) {
            target.removeEffect(com.phagens.corpseorigin.register.EffectRegister.QIANS);
        }
    }

    private static class DefaultCorpseWaterHandler implements ICorpseWaterHandler {

        @Override
        public net.minecraft.world.level.material.Fluid getCorpseWaterFluid() {
            return Fluids.WATER;
        }

        @Override
        public boolean isCorpseWater(net.minecraft.world.level.material.Fluid fluid) {
            return fluid == Fluids.WATER;
        }

        @Override
        public ItemStack createCorpseWaterBottle() {
            return new ItemStack(Moditems.BYWATER_BOTTLE.get());
        }

        @Override
        public ItemStack createCorpseWaterBucket() {
            return new ItemStack(Moditems.BYWATER_BUCKET.get());
        }

        @Override
        public boolean canBeConvertedToCorpseWater(ServerLevel level, BlockPos pos) {
            return level.getBlockState(pos).getFluidState().is(Fluids.WATER);
        }

        @Override
        public void convertWaterToCorpseWater(ServerLevel level, BlockPos pos) {
            InfectionData.markWaterInfectedStatic(level, pos);
        }

        @Override
        public void convertCorpseWaterToCleanWater(ServerLevel level, BlockPos pos) {
            InfectionData.get(level).setWaterEnergy(pos, 0);
        }

        @Override
        public int getCorpseWaterEnergy(ServerLevel level, BlockPos pos) {
            return InfectionData.get(level).getWaterEnergy(pos);
        }

        @Override
        public void setCorpseWaterEnergy(ServerLevel level, BlockPos pos, int energy) {
            InfectionData.get(level).setWaterEnergy(pos, energy);
        }

        @Override
        public int getMaxCorpseWaterEnergy() {
            return 15;
        }

        @Override
        public FluidStack extractFluidFromPosition(ServerLevel level, BlockPos pos, int amount) {
            if (isPositionCorpseWaterSource(level, pos)) {
                return new FluidStack(getCorpseWaterFluid(), amount);
            }
            return new FluidStack(Fluids.WATER, amount);
        }

        @Override
        public boolean isPositionCorpseWaterSource(ServerLevel level, BlockPos pos) {
            return InfectionData.isWaterInfectedStatic(level, pos) || level.getBiome(pos).is(BiomeRegistry.DEAD_SILENCE);
        }
    }
}