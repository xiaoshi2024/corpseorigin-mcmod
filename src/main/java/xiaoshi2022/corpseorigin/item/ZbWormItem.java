package xiaoshi2022.corpseorigin.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.item.ZbWormItemRenderer;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.effect.BYeffect;
import xiaoshi2022.corpseorigin.entity.ZbWormEntity;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 尸兄虫物品 - 可食用的虫子。
 * <p>
 * 尸兄玩家食用回饱食度并概率吃出增益；普通玩家食用大概率被虫子咬死、小概率当场感染成尸兄。
 */
public class ZbWormItem extends Item implements GeoItem {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int NUTRITION = 3;
    private static final float SATURATION = 0.2F;

    /** 尸兄食用时，速度增益的触发概率 */
    private static final float SPEED_BOOST_CHANCE = 0.3F;
    /** 尸兄食用时，挖掘效率增益的触发概率 */
    private static final float DIG_SPEED_BOOST_CHANCE = 0.2F;

    /** 尸兄食用恢复的尸兄饥饿值 */
    private static final int CORPSE_HUNGER_RESTORE = 15;

    /** 普通玩家食用：死亡概率 */
    private static final double DEATH_CHANCE = 0.7;

    public ZbWormItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(NUTRITION)
                .saturationModifier(SATURATION)
                .alwaysEdible()
                .build()));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (PlayerCorpseComponent.isCorpse(player)) {
                onWormConsumed(player);
            } else {
                onNormalPlayerConsume(player);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** 尸兄食用：回饱食度，概率拿到增益 */
    private void onWormConsumed(Player player) {
        CorpseOrigin.LOGGER.info("尸兄玩家 {} 食用了尸兄虫子", player.getName().getString());

        PlayerCorpseComponent corpse = PlayerCorpseComponent.get(player);
        corpse.setHunger(Math.min(100, corpse.getHunger() + CORPSE_HUNGER_RESTORE));

        boolean gainedEffect = false;

        if (player.getRandom().nextFloat() < SPEED_BOOST_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 3000, 1));
            gainedEffect = true;
        }
        if (player.getRandom().nextFloat() < DIG_SPEED_BOOST_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 3000, 1));
            gainedEffect = true;
        }

        player.sendOverlayMessage(Component.translatable(gainedEffect
                        ? "item.corpseorigin.zb_worm.effect_gained"
                        : "item.corpseorigin.zb_worm.consumed")
                .withStyle(gainedEffect ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    /** 普通玩家食用：大概率横死，小概率原地感染 */
    private void onNormalPlayerConsume(Player player) {
        CorpseOrigin.LOGGER.info("普通玩家 {} 食用了尸兄虫子，开始判定...", player.getName().getString());

        // 已经是尸兄（理论上走不到这里，防重复感染）
        if (PlayerCorpseComponent.isCorpse(player)) {
            return;
        }

        // 体内已经有虫子在变异了，不再叠加
        if (player.hasEffect(ModEffects.QIANS)) {
            player.sendOverlayMessage(Component.literal("你体内已经有虫子在蠕动了...")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (player.getRandom().nextDouble() < DEATH_CHANCE) {
            player.sendOverlayMessage(Component.literal("")
                    .append(Component.literal("虫子在你体内疯狂撕咬！你的内脏被彻底摧毁...")
                            .withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal("\n§c你死了...")));

            if (player.level() instanceof ServerLevel serverLevel) {
                player.hurtServer(serverLevel, player.damageSources().magic(), Float.MAX_VALUE);
            }
            player.setHealth(0);
            CorpseOrigin.LOGGER.info("玩家 {} 食用尸兄虫子后死亡", player.getName().getString());
        } else {
            player.sendOverlayMessage(Component.literal("")
                    .append(Component.literal("你感觉有什么东西在你体内蠕动...")
                            .withStyle(ChatFormatting.DARK_RED))
                    .append(Component.literal("\n§c你的身体正在发生可怕的变化！")));

            if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
                BYeffect.applyInfection(serverPlayer, serverLevel);
                CorpseOrigin.LOGGER.info("玩家 {} 食用尸兄虫子后感染成功", player.getName().getString());
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.corpseorigin.zb_worm.description")
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.corpseorigin.zb_worm.effect1",
                        (int) (SPEED_BOOST_CHANCE * 100))
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.corpseorigin.zb_worm.effect2",
                        (int) (DIG_SPEED_BOOST_CHANCE * 100))
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.empty());
        builder.accept(Component.translatable("item.corpseorigin.zb_worm.corpse_bonus")
                .withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable("item.corpseorigin.zb_worm.not_corpse_warning")
                .withStyle(ChatFormatting.RED));

        super.appendHoverText(stack, context, display, builder, flag);
    }

    // ==================== GeoItem 接口 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ZbWormItem>("idle_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationTest<ZbWormItem> test) {
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ZbWormItemRenderer renderer;

            @Override
            public @Nullable GeoItemRenderer<ZbWormItem> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ZbWormItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * 「丢出去变回活体虫子」的孵化检查：这坨掉落物是尸兄虫的话，把它换成活体虫子。
     * <p>
     * 由 {@code ItemEntityMixin} 在掉落物出生后的第一帧服务端 tick 调用 ——
     * 1.21.1 那边靠的是 NeoForge 的 {@code Item#onDroppedByPlayer}（能在掉落物生成之前就拦下来），
     * Fabric 没有这个钩子，26.2 的 fabric-entity-events 也已经把 {@code ENTITY_LOAD} 删掉了，
     * 所以退一步：让掉落物自己"孵"一下。
     *
     * @return true = 已经换成虫子了
     */
    public static boolean tryReleaseWorm(ItemEntity itemEntity) {
        if (!(itemEntity.level() instanceof ServerLevel level)) {
            return false;
        }

        int count = Math.max(1, itemEntity.getItem().getCount());

        // 先把虫子全部造好：万一造不出来（实体类型没注册之类）就整坨留在地上，
        // 不能出现"物品被吞了、虫子没出来"的情况
        List<ZbWormEntity> worms = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ZbWormEntity worm = ModEntities.ZB_WORM.create(level, EntitySpawnReason.TRIGGERED);
            if (worm == null) {
                return false;
            }
            worms.add(worm);
        }

        Vec3 pos = itemEntity.position();
        float yRot = itemEntity.getYRot();
        itemEntity.discard();

        // 一坨里有几只就放几只：既不凭空吞物品，也不会留下半坨在下次区块加载时又孵一遍
        for (ZbWormEntity worm : worms) {
            worm.snapTo(pos.x, pos.y + 0.05, pos.z, yRot, 0.0F);
            level.addFreshEntity(worm);
            worm.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.0F, 1.0F);
        }

        return true;
    }
}
