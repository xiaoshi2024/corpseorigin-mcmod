//package xiaoshi2022.corpseorigin.item;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.particles.ParticleTypes;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Items;
//import net.minecraft.world.level.ClipContext;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Blocks;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.phys.BlockHitResult;
//import xiaoshi2022.corpseorigin.infection.InfectionManager;
//
///**
// * 尸水桶 - 放置满感染能量的水源（参考1.21.1 ByWaterBucketItem）
// */
//public class ByWaterBucketItem extends Item {
//
//    public ByWaterBucketItem(Properties properties) {
//        super(properties);
//    }
//
//    @Override
//    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
//        ItemStack stack = player.getItemInHand(hand);
//        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
//
//        if (hit.getType() == BlockHitResult.Type.MISS) {
//            return InteractionResult.PASS;
//        }
//
//        BlockPos placePos = hit.getBlockPos();
//        BlockState existing = level.getBlockState(placePos);
//
//        if (existing.isAir() || existing.canBeReplaced()) {
//            // 直接命中空气/可替换方块：放在命中处
//        } else {
//            // 命中方块：尝试放置在命中面相邻处
//            BlockPos offset = placePos.relative(hit.getDirection());
//            BlockState offsetState = level.getBlockState(offset);
//            if (!offsetState.isAir() && !offsetState.canBeReplaced()) {
//                return InteractionResult.FAIL;
//            }
//            placePos = offset;
//        }
//
//        if (level instanceof ServerLevel serverLevel) {
//            level.setBlock(placePos, Blocks.WATER.defaultBlockState(), 3);
//            InfectionManager.markWater(serverLevel, placePos);
//            serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
//                    placePos.getX() + 0.5, placePos.getY() + 0.8, placePos.getZ() + 0.5,
//                    10, 0.2, 0.2, 0.2, 0.0);
//            serverLevel.playSound(null, placePos, SoundEvents.BUCKET_EMPTY,
//                    SoundSource.PLAYERS, 1.0f, 0.8f);
//
//            if (!player.getAbilities().instabuild) {
//                stack.consume(1, player);
//                ItemStack bucket = new ItemStack(Items.BUCKET);
//                if (!player.getInventory().add(bucket)) {
//                    player.drop(bucket, false);
//                }
//            }
//        }
//        return InteractionResult.SUCCESS;
//    }
//}
