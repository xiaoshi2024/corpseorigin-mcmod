package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModFluids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InfectedWaterBlock extends LiquidBlock {

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final long POISON_COOLDOWN = 3000L;

    public InfectedWaterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
        // ✅ 父类 LiquidBlock 已经包含了 LEVEL 属性，不需要再添加
    }

    // ❌ 删除 createBlockStateDefinition - 父类已经包含了 LEVEL
    // 不要重写这个方法！

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isMoving) {
        super.entityInside(state, level, pos, entity, effectApplier, isMoving);

        if (level.isClientSide()) {
            return;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() != ModFluids.INFECTED_WATER &&
                fluidState.getType() != ModFluids.FLOWING_INFECTED_WATER) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (living instanceof Player player) {
            // 龙右是「尸水之源」，完全免疫尸水；已经是尸兄的泡在里面也没反应
            if (LongYou.isImmuneToInfectedWater(player) || PlayerCorpseComponent.isCorpse(player)) {
                return;
            }

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
                CorpseOrigin.LOGGER.debug("玩家 {} 接触尸水，中毒", player.getName().getString());
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
                CorpseOrigin.LOGGER.info("村民 {} 接触尸水，被感染", villager.getName().getString());
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            scheduleOceanDilution(level, pos);
            CorpseOrigin.LOGGER.debug("尸水方块放置于: {}", pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        int delay = CorpseConfig.get().infectedWater.oceanDilutionTicks;
        if (delay <= 0 || !isInOcean(level, pos)) {
            return;
        }
        level.setBlock(pos, Fluids.WATER.defaultFluidState().createLegacyBlock(),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            scheduleOceanDilution(level, pos);
        }
    }

    private void scheduleOceanDilution(Level level, BlockPos pos) {
        int delay = CorpseConfig.get().infectedWater.oceanDilutionTicks;
        if (delay > 0 && isInOcean(level, pos)) {
            level.scheduleTick(pos, this, delay);
        }
    }

    private boolean isInOcean(Level level, BlockPos pos) {
        return level.getBiome(pos).is(BiomeTags.IS_OCEAN);
    }
}
