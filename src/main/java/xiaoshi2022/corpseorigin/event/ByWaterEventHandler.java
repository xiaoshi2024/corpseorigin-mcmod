package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.registry.ModItems;

public final class ByWaterEventHandler {

    private ByWaterEventHandler() {
    }

    public static void register() {
        // ===== 使用物品事件：只处理玻璃瓶 =====
        // ✅ 桶交给原版 BucketItem 处理（它会自动检查 LEVEL）
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack heldItem = player.getItemInHand(hand);

            // ✅ 只处理玻璃瓶！桶完全交给原版
            if (heldItem.getItem() != Items.GLASS_BOTTLE) {
                return InteractionResult.PASS;
            }

            BlockHitResult hitResult = getPlayerPOVHitResult(level, player);
            if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
                return InteractionResult.PASS;
            }

            BlockPos targetPos = hitResult.getBlockPos();
            BlockState blockState = level.getBlockState(targetPos);
            FluidState fluidState = blockState.getFluidState();

            // 检查是否是水
            if (!fluidState.is(FluidTags.WATER)) {
                return InteractionResult.PASS;
            }

            // 检查是否是尸水
            boolean isInfectedWater = fluidState.getType() == ModFluids.INFECTED_WATER
                    || fluidState.getType() == ModFluids.FLOWING_INFECTED_WATER;

            if (!isInfectedWater) {
                return InteractionResult.PASS;  // 普通水交给原版
            }

            // ✅ 只有源块（LEVEL == 0）才能被舀取
            //    克隆仓这类"自带流体状态但不是流体方块"的方块没有 LEVEL 属性，先挡掉，别在这里抛异常
            if (!blockState.hasProperty(LiquidBlock.LEVEL) || blockState.getValue(LiquidBlock.LEVEL) != 0) {
                return InteractionResult.PASS;  // 流动水 / 非流体方块不能舀取
            }

            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }

            // ===== 玻璃瓶舀取尸水源 =====
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            ItemStack bywaterBottle = new ItemStack(ModItems.BYWATER_BOTTLE);

            ItemStack resultStack;
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, bywaterBottle);
                resultStack = bywaterBottle;
            } else {
                if (!player.getInventory().add(bywaterBottle)) {
                    player.drop(bywaterBottle, false);
                }
                resultStack = heldItem;
            }

            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);

            CorpseOrigin.LOGGER.info("玩家 {} 用玻璃瓶舀取尸水源", player.getName().getString());
            return InteractionResult.SUCCESS.heldItemTransformedTo(resultStack);
        });
    }

    private static BlockHitResult getPlayerPOVHitResult(Level level, Player player) {
        double reach = player.blockInteractionRange();
        return level.clip(
                new net.minecraft.world.level.ClipContext(
                        player.getEyePosition(),
                        player.getEyePosition().add(player.getViewVector(1.0F).scale(reach)),
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.ANY,
                        player
                )
        );
    }
}