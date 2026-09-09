package xiaoshi2022.corpseorigin.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class InfectedWaterFluid extends FlowingFluid {

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final long POISON_COOLDOWN = 3000L;

    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_INFECTED_WATER;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.INFECTED_WATER;
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 1;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return other == ModFluids.INFECTED_WATER || other == ModFluids.FLOWING_INFECTED_WATER;
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.INFECTED_WATER || fluid == ModFluids.FLOWING_INFECTED_WATER;
    }

    @Override
    protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
        // ✅ 熄灭火焰（像水一样）
        effectApplier.apply(InsideBlockEffectType.EXTINGUISH);

        if (level.isClientSide()) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (living instanceof Player player) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastTime = playerCooldowns.get(playerId);

            if (lastTime == null || (currentTime - lastTime) >= POISON_COOLDOWN) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.POISON,
                        100,
                        0
                ));
                playerCooldowns.put(playerId, currentTime);
            }
            return;
        }

        if (living instanceof Villager villager) {
            if (!villager.hasEffect(ModEffects.QIANS)) {
                villager.addEffect(new MobEffectInstance(
                        ModEffects.QIANS,
                        200,
                        0,
                        false,
                        true,
                        true
                ));
            }
        }
    }

    // ==================== 流动尸水 ====================

    public static class Flowing extends InfectedWaterFluid {

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public Item getBucket() {
            return ModItems.BYWATER_BUCKET;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState fluidState) {
            // ✅ 使用 BlockStateProperties.LEVEL
            return ModFluids.INFECTED_WATER_BLOCK.defaultBlockState()
                    .setValue(BlockStateProperties.LEVEL, getLegacyLevel(fluidState));
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        @Override
        public int getAmount(FluidState fluidState) {
            return fluidState.getValue(LEVEL);
        }

        @Override
        public FluidState getSource(boolean falling) {
            return ModFluids.INFECTED_WATER.defaultFluidState().setValue(FALLING, falling);
        }

        @Override
        public FluidState getFlowing(int amount, boolean falling) {
            return ModFluids.FLOWING_INFECTED_WATER.defaultFluidState()
                    .setValue(LEVEL, amount)
                    .setValue(FALLING, falling);
        }
    }

    // ==================== 尸水源块 ====================

    public static class Source extends InfectedWaterFluid {

        @Override
        public Item getBucket() {
            return ModItems.BYWATER_BUCKET;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState fluidState) {
            // ✅ 源块不需要设置 LEVEL
            return ModFluids.INFECTED_WATER_BLOCK.defaultBlockState();
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState fluidState) {
            return 8;
        }

        @Override
        public FluidState getSource(boolean falling) {
            return ModFluids.INFECTED_WATER.defaultFluidState().setValue(FALLING, falling);
        }

        @Override
        public FluidState getFlowing(int amount, boolean falling) {
            return ModFluids.FLOWING_INFECTED_WATER.defaultFluidState()
                    .setValue(LEVEL, amount)
                    .setValue(FALLING, falling);
        }
    }
}