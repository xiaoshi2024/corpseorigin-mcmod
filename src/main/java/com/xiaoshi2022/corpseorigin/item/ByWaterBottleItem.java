package com.xiaoshi2022.corpseorigin.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import com.xiaoshi2022.corpseorigin.registry.ModEffects;

/** 尸水瓶：使用/饮用逻辑（1.20.1 Forge）。 */
public class ByWaterBottleItem extends Item {

    public ByWaterBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof Player player) {
            if (target instanceof Villager) {
                target.addEffect(new MobEffectInstance(ModEffects.QIANS.get(), 200, 0, false, true, true));
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.getInventory().add(bottle)) player.drop(bottle, false);
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            if (entity instanceof Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1, false, true, true));
                player.addEffect(new MobEffectInstance(ModEffects.QIANS.get(), 200, 0, false, true, true));
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.5F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
