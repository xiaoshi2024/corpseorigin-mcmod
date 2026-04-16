package com.phagens.corpseorigin.Item.zbritem;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.Renderer.item.ZbWormitemRenderer;
import com.phagens.corpseorigin.effect.BYeffect;
import com.phagens.corpseorigin.entity.Animals.ZbWormEntity;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * 尸兄虫子物品 - 可食用的虫子
 * 普通玩家食用后会被感染成为尸兄！
 * 尸兄玩家食用可以恢复饥饿度和获得增益
 * 丢弃后会重新变成实体
 */
public class ZbWormitem extends Item implements GeoItem {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 饥饿值恢复
    private static final int NUTRITION = 3;
    // 饱和度恢复
    private static final float SATURATION = 0.2f;
    // 速度增益概率
    private static final float SPEED_BOOST_CHANCE = 0.3f;
    // 挖掘效率增益概率
    private static final float DIG_SPEED_BOOST_CHANCE = 0.2f;

    public ZbWormitem() {
        super(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(NUTRITION)
                        .saturationModifier(SATURATION)
                        .alwaysEdible()
                        .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            // 尸兄玩家食用效果
            if (PlayerCorpseData.isCorpse(player)) {
                onWormConsumed(player, stack);
            } else {
                // 普通玩家食用 - 感染成为尸兄！
                onNormalPlayerConsume(player, stack);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /**
     * 尸兄玩家食用虫子时的处理
     */
    protected void onWormConsumed(Player player, ItemStack stack) {
        CorpseOrigin.LOGGER.info("尸兄玩家 {} 食用了尸兄虫子", player.getName().getString());

        // 恢复饥饿值
        PlayerCorpseData.setHunger(player, Math.min(100, PlayerCorpseData.getHunger(player) + 15));

        // 随机给予效果
        boolean gainedEffect = false;

        // 速度增益
        if (player.getRandom().nextFloat() < SPEED_BOOST_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3000, 1));
            gainedEffect = true;
        }

        // 挖掘效率增益
        if (player.getRandom().nextFloat() < DIG_SPEED_BOOST_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 3000, 1));
            gainedEffect = true;
        }

        if (gainedEffect) {
            player.sendSystemMessage(Component.translatable("item.corpseorigin.zb_worm.effect_gained")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("item.corpseorigin.zb_worm.consumed")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 普通玩家食用虫子 - 大概率死亡，小概率感染
     */
    protected void onNormalPlayerConsume(Player player, ItemStack stack) {
        CorpseOrigin.LOGGER.info("普通玩家 {} 食用了尸兄虫子，开始判定...", player.getName().getString());

        // 检查玩家是否已经是尸兄（防止重复感染）
        if (PlayerCorpseData.isCorpse(player)) {
            return;
        }

        // 检查玩家是否已经被感染中
        if (player.hasEffect(com.phagens.corpseorigin.register.EffectRegister.QIANS)) {
            player.sendSystemMessage(Component.literal("你体内已经有虫子在蠕动了...")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 死亡概率 70%，感染概率 30%（可调整）
        double deathChance = 0.7;  // 70% 死亡
        double infectionChance = 0.3; // 30% 感染

        // 直接使用 player.getRandom()，它已经是 RandomSource 类型，有 nextDouble() 方法
        double roll = player.getRandom().nextDouble();

        if (roll < deathChance) {
            // 大概率：死亡
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal("虫子在你体内疯狂撕咬！你的内脏被彻底摧毁...").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal("\n§c你死了...")));

            // 造成巨额伤害致死
            player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
            // 确保死亡
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.setHealth(0);
            }

            CorpseOrigin.LOGGER.info("玩家 {} 食用尸兄虫子后死亡", player.getName().getString());
        } else {
            // 小概率：感染成为尸兄
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal("你感觉有什么东西在你体内蠕动...").withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal("\n§c你的身体正在发生可怕的变化！")));

            if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
                // 调用感染系统的应用方法
                BYeffect.applyInfection(serverPlayer, serverLevel);
                CorpseOrigin.LOGGER.info("玩家 {} 食用尸兄虫子后感染成功", player.getName().getString());
            } else {
                // 备用方案：给中毒效果
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
    }

    /**
     * 物品被丢弃时触发 - 重新变成实体
     */
    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        // 不在客户端执行
        if (player.level().isClientSide) {
            return false;
        }

        // 在玩家位置生成虫子实体
        if (player.level() instanceof ServerLevel serverLevel) {
            // 检查实体类型是否已注册
            if (EntityRegistry.ZB_WORM.get() != null) {
                // 先减少物品数量（模拟丢弃消耗）
                item.shrink(1);

                // 生成虫子实体
                ZbWormEntity worm = new ZbWormEntity(EntityRegistry.ZB_WORM.get(), serverLevel);
                worm.setPos(player.getX(), player.getY(), player.getZ());
                worm.setYRot(player.getYRot());

                // 添加实体到世界
                serverLevel.addFreshEntity(worm);

                // 播放声音效果
                worm.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.0F, 1.0F);

                CorpseOrigin.LOGGER.info("玩家 {} 丢弃了尸兄虫子，已重新生成实体", player.getName().getString());

                // 返回 false 防止原物品实体生成（我们已经手动处理了物品减少）
                return false;
            } else {
                CorpseOrigin.LOGGER.error("ZB_WORM_ENTITY 未注册，无法生成实体！");
            }
        }

        // 如果实体生成失败，允许正常丢弃物品
        return true;
    }

    /**
     * 物品实体在世界上tick时的处理
     * 可以用于自动复活机制（可选）
     */
    public static void onItemEntityTick(ItemEntity itemEntity) {
        // 可选：如果虫子物品在地上太久没被捡起，自动变成实体
        // 这里先不做自动复活，保持简单
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.translatable("item.corpseorigin.zb_worm.description")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.corpseorigin.zb_worm.effect1",
                        (int)(SPEED_BOOST_CHANCE * 100))
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.corpseorigin.zb_worm.effect2",
                        (int)(DIG_SPEED_BOOST_CHANCE * 100))
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.corpseorigin.zb_worm.corpse_bonus")
                .withStyle(ChatFormatting.GREEN));
        // 添加感染警告
        tooltipComponents.add(Component.literal("§c⚠ 普通玩家食用后会被感染成为尸兄！"));
        // 添加丢弃提示
        tooltipComponents.add(Component.literal("§7丢弃后会重新变成活生生的虫子！").withStyle(ChatFormatting.GRAY));
    }

    // GeoItem 接口实现
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> {
            state.getController().setAnimation(IDLE_ANIM);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ZbWormitemRenderer renderer;

            @Override
            public GeoItemRenderer<ZbWormitem> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ZbWormitemRenderer();
                }
                return this.renderer;
            }
        });
    }
}