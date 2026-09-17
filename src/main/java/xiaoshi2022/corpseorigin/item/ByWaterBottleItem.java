package xiaoshi2022.corpseorigin.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.registry.ModEffects;

public class ByWaterBottleItem extends Item {

    public ByWaterBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 故意不做 canEat 检查：这瓶东西是"疫病源"，不是食物，
        // 饱食度满了也应该能随时喝下去（原版食物行为会在这里把人拦住）。
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof Player player) {
            if (target instanceof Villager) {
                target.addEffect(new MobEffectInstance(
                        ModEffects.QIANS,
                        200,
                        0,
                        false,
                        true,
                        true
                ));

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                }
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // finishUsingItem 只在服务端触发（客户端的 useItemRemaining 归零不会走这里），
        // 所以这些效果与音效都放在服务端，playSound(null, ...) 会广播给周围玩家（含自己）。
        if (!level.isClientSide()) {
            if (entity instanceof Player player) {
                // 龙右是「尸水之源」，完全免疫尸水；
                // 已经是尸兄的再喝也没反应（不然会被 QIANS 拉去重走一遍变异流程）
                if (!LongYou.isImmuneToInfectedWater(player)
                        && !PlayerCorpseComponent.isCorpse(player)) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.POISON,
                            400,
                            1,
                            false,
                            true,
                            true
                    ));

                    player.addEffect(new MobEffectInstance(
                            ModEffects.QIANS,
                            200,
                            0,
                            false,
                            true,
                            true
                    ));
                }
            }

            // 喝下的音效 —— 原本缺了这一段，所以喝下去是"静音"的。
            // 参数照原版喝药水（Consumable 组件里的那颗）。
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.5F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);
        }

        return new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }
}