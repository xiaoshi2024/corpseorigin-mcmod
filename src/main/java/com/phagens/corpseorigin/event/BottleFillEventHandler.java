package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.data.InfectionData;
import com.phagens.corpseorigin.register.BiomeRegistry;
import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class BottleFillEventHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) {
            return;
        }

        ItemStack heldItem = event.getItemStack();
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide) {
            return;
        }

        // 检查玩家视线是否对准水方块
        BlockHitResult hitResult = level.clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getViewVector(1.0F).scale(5.0F)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        if (!level.getBlockState(targetPos).getFluidState().is(FluidTags.WATER)) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 检查条件：水源被感染（InfectionData记录） 或 在死寂群系中
        boolean isWaterInfected = InfectionData.isWaterInfectedStatic(serverLevel, targetPos);
        boolean isInCorpseBiome = serverLevel.getBiome(targetPos).is(BiomeRegistry.DEAD_SILENCE);

        if (!isWaterInfected && !isInCorpseBiome) {
            return;
        }

        // 玻璃瓶舀取尸水
        if (heldItem.getItem() == Items.GLASS_BOTTLE) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
            handleBottleFill(player, event.getHand(), heldItem);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        // 铁桶舀取尸水
        else if (heldItem.getItem() == Items.BUCKET) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
            handleBucketFill(player, event.getHand(), heldItem);
            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * 处理铁桶舀取尸水
     * @param player 玩家
     * @param hand 手部
     * @param bucketStack 铁桶物品栈
     */
    private static void handleBucketFill(Player player, net.minecraft.world.InteractionHand hand, ItemStack bucketStack) {
        if (!player.getAbilities().instabuild) {
            bucketStack.shrink(1);
        }

        ItemStack bywaterBucket = new ItemStack(Moditems.BYWATER_BUCKET.get());

        if (bucketStack.isEmpty()) {
            player.setItemInHand(hand, bywaterBucket);
        } else {
            if (!player.getInventory().add(bywaterBucket)) {
                player.drop(bywaterBucket, false);
            }
        }
    }

    /**
     * 处理玻璃瓶舀取尸水
     * @param player 玩家
     * @param hand 手部
     * @param bottleStack 玻璃瓶物品栈
     */
    private static void handleBottleFill(Player player, net.minecraft.world.InteractionHand hand, ItemStack bottleStack) {
        if (!player.getAbilities().instabuild) {
            // 生存模式：减少1个玻璃瓶
            bottleStack.shrink(1);
        }

        // 获取尸水瓶
        ItemStack bywaterBottle = new ItemStack(Moditems.BYWATER_BOTTLE.get());

        // 将尸水瓶添加到玩家背包
        if (bottleStack.isEmpty()) {
            // 如果玻璃瓶用完，直接替换手中的物品
            player.setItemInHand(hand, bywaterBottle);
        } else {
            // 如果玻璃瓶还有剩余，尝试添加到背包
            if (!player.getInventory().add(bywaterBottle)) {
                // 背包满了，掉落在地上
                player.drop(bywaterBottle, false);
            }
        }
    }
}