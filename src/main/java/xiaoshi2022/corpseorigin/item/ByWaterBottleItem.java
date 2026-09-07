//package xiaoshi2022.corpseorigin.item;
//
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.effect.MobEffects;
//import net.minecraft.world.entity.EntitySpawnReason;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.npc.villager.Villager;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.ItemUseAnimation;
//import net.minecraft.world.item.Items;
//import net.minecraft.world.level.Level;
//import xiaoshi2022.corpseorigin.infection.InfectionManager;
//import xiaoshi2022.corpseorigin.registry.ModEntities;
//
///**
// * 尸水瓶 - 饮用大幅累积感染，用其攻击村民可将其感染为尸兄
// */
//public class ByWaterBottleItem extends Item {
//
//    public ByWaterBottleItem(Properties properties) {
//        super(properties);
//    }
//
//    @Override
//    public InteractionResult use(Level level, Player player, InteractionHand hand) {
//        player.startUsingItem(hand);
//        return InteractionResult.CONSUME;
//    }
//
//    @Override
//    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
//        if (!level.isClientSide() && entity instanceof Player player) {
//            // 饮用：大量感染
//            InfectionManager.addInfection(player, 50, player.getUUID());
//            player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 1, false, true));
//            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0, false, true));
//            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(),
//                    SoundSource.PLAYERS, 1.0f, 0.7f);
//        }
//        return new ItemStack(Items.GLASS_BOTTLE);
//    }
//
//    @Override
//    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
//        if (!attacker.level().isClientSide() && attacker instanceof Player player) {
//            if (target instanceof Villager villager && attacker.level() instanceof ServerLevel serverLevel) {
//                // 感染村民 → 自定义尸兄实体
//                DefaultCorpseEntity corpse = ModEntities.DEFAULT_CORPSE.create(serverLevel, EntitySpawnReason.CONVERSION);
//                if (corpse != null) {
//                    // 复制位置和旋转
//                    corpse.snapTo(villager.getX(), villager.getY(), villager.getZ(),
//                            villager.getYRot(), villager.getXRot());
//
//                    // 设置皮肤名称为村民的名字（如果有）
//                    if (villager.hasCustomName()) {
//                        corpse.setPlayerSkinName(villager.getCustomName().getString());
//                    } else {
//                        corpse.setPlayerSkinName("villager");
//                    }
//
//                    // 设置进化等级（随机1-2级）
//                    corpse.setEvolutionLevel(1 + serverLevel.getRandom().nextInt(2));
//
//                    // 有概率成为精英
//                    if (serverLevel.getRandom().nextFloat() < 0.1f) {
//                        corpse.setElite(true);
//                    }
//
//                    // 移除村民，添加尸兄
//                    villager.discard();
//                    serverLevel.addFreshEntity(corpse);
//                    serverLevel.playSound(null, corpse.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_AMBIENT,
//                            SoundSource.PLAYERS, 1.0f, 0.8f);
//                }
//
//                if (!player.getAbilities().instabuild) {
//                    stack.consume(1, player);
//                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
//                    if (!player.getInventory().add(bottle)) {
//                        player.drop(bottle, false);
//                    }
//                }
//            } else if (target instanceof Player targetPlayer) {
//                // 攻击其他玩家：大幅累积感染度
//                InfectionManager.addInfection(targetPlayer, 30, player.getUUID());
//            }
//        }
//    }
//
//    @Override
//    public int getUseDuration(ItemStack stack, LivingEntity entity) {
//        return 32;
//    }
//
//    @Override
//    public ItemUseAnimation getUseAnimation(ItemStack stack) {
//        return ItemUseAnimation.DRINK;
//    }
//}