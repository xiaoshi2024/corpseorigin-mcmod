package com.xiaoshi2022.corpseorigin.registry;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.block.InfectedWaterBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

/**
 * 尸水流体 + 方块注册（1.20.1 Forge）。
 * <p>
 * 继承 {@link WaterFluid.Source} / {@link WaterFluid.Flowing} 以获得 LEVEL 属性与全部流动性实现，
 * 但必须覆盖以下三个方法，否则尸水会退化成原版水：
 * <ul>
 *   <li>{@code createLegacyBlock} —— 原版 WaterFluid 返回 {@code Blocks.WATER}，桶倒出来就是普通水</li>
 *   <li>{@code isSame} —— 原版只认 minecraft:water / flowing_water，导致同流体判定失败</li>
 *   <li>{@code getBucket} —— 指向尸水桶</li>
 * </ul>
 */
public final class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, CorpseOrigin.MODID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CorpseOrigin.MODID);

    public static final RegistryObject<FlowingFluid> INFECTED_WATER = FLUIDS.register("infected_water",
            InfectedSource::new);

    public static final RegistryObject<FlowingFluid> FLOWING_INFECTED_WATER = FLUIDS.register("flowing_infected_water",
            InfectedFlowing::new);

    /** 方块 ID 与资源 blockstates/models/lang 保持一致。 */
    public static final RegistryObject<LiquidBlock> INFECTED_WATER_BLOCK = BLOCKS.register("infected_water_block",
            () -> new InfectedWaterBlock(INFECTED_WATER.get(),
                    // 与原版水方块保持一致的属性：replaceable 让你能用方块把水源堵上，
                    // liquid 让渲染/摄像机/雾效按液体处理，pushReaction 让活塞能推掉。
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100.0F)
                            .noLootTable()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                            .liquid()
                            .replaceable()));

    private static final ResourceLocation STILL_TEXTURE =
            new ResourceLocation(CorpseOrigin.MODID, "block/infected_water_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            new ResourceLocation(CorpseOrigin.MODID, "block/infected_water_flow");
    private static final ResourceLocation OVERLAY_TEXTURE =
            new ResourceLocation(CorpseOrigin.MODID, "block/infected_water_overlay");

    /** 尸水属性 + 客户端贴图（Forge 流体渲染走 IClientFluidTypeExtensions，不走方块模型）。 */
    private static final FluidType INFECTED_WATER_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("block.corpseorigin.infected_water_block")
            .canConvertToSource(false)
            .canDrown(true)
            .canSwim(true)
            .canPushEntity(true)
            .canExtinguish(true)
            .canHydrate(false)
            .supportsBoating(false)
            .lightLevel(0)
            .density(1000)
            .viscosity(1200)
            .temperature(300)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return STILL_TEXTURE; }
                @Override public ResourceLocation getFlowingTexture() { return FLOWING_TEXTURE; }
                @Override public ResourceLocation getOverlayTexture() { return OVERLAY_TEXTURE; }
                @Override public int getTintColor() {
                    return com.xiaoshi2022.corpseorigin.config.CorpseOriginConfig.fluidTint();
                }
            });
        }
    };

    private ModFluids() {}

    private static BlockState legacyBlock(FluidState state) {
        // 等价于 FlowingFluid#getLegacyLevel（protected，外部不可调用）
        int level = state.isSource()
                ? 0
                : 8 - Math.min(state.getAmount(), 8) + (state.getValue(FlowingFluid.FALLING) ? 8 : 0);
        return INFECTED_WATER_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, level);
    }

    private static boolean isInfectedFluid(Fluid fluid) {
        return fluid == INFECTED_WATER.get() || fluid == FLOWING_INFECTED_WATER.get();
    }

    // ========== 流体实现 ==========

    public static class InfectedSource extends WaterFluid.Source {
        @Override public Fluid getFlowing() { return FLOWING_INFECTED_WATER.get(); }
        @Override public Fluid getSource() { return INFECTED_WATER.get(); }
        @Override public Item getBucket() { return ModItems.BYWATER_BUCKET.get(); }
        @Override public FluidType getFluidType() { return INFECTED_WATER_TYPE; }
        @Override public BlockState createLegacyBlock(FluidState state) { return legacyBlock(state); }
        @Override public boolean isSame(Fluid fluid) { return isInfectedFluid(fluid); }
    }

    public static class InfectedFlowing extends WaterFluid.Flowing {
        @Override public Fluid getFlowing() { return FLOWING_INFECTED_WATER.get(); }
        @Override public Fluid getSource() { return INFECTED_WATER.get(); }
        @Override public Item getBucket() { return ModItems.BYWATER_BUCKET.get(); }
        @Override public FluidType getFluidType() { return INFECTED_WATER_TYPE; }
        @Override public BlockState createLegacyBlock(FluidState state) { return legacyBlock(state); }
        @Override public boolean isSame(Fluid fluid) { return isInfectedFluid(fluid); }
    }
}
