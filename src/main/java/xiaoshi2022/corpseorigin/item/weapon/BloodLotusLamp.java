package xiaoshi2022.corpseorigin.item.weapon;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.item.BloodLotusLampRenderer;
import xiaoshi2022.corpseorigin.network.BloodLotusLaserPayload;

import java.util.List;
import java.util.function.Consumer;

/**
 * 血莲宝灯（天外神陨 血梅·宝莲灯）
 * <p>
 * 血莲教镇教宝灯，由天外神陨制作，可以吸收敌人的气血之力。
 * <p>
 * 长按持续吸血，持续消耗耐久，并向视线方向发射红色激光。
 */
public class BloodLotusLamp extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 吸血范围 */
    private static final double DRAIN_RANGE = 8.0;
    /** 每次吸血伤害 */
    private static final float DRAIN_DAMAGE = 2.0F;
    /** 吸血回复比例 */
    private static final float HEAL_RATIO = 0.5F;
    /** 每隔多少 tick 触发一次吸血（20 tick = 1 秒） */
    private static final int DRAIN_INTERVAL = 20;
    /** 最长长按时间（ticks）—— 5 秒，防止无限吸 */
    private static final int MAX_USE_DURATION = 100;

    public BloodLotusLamp(Properties properties) {
        super(properties);
    }

    public static BloodLotusLamp create(ResourceKey<Item> id) {
        return new BloodLotusLamp(new Item.Properties()
                .stacksTo(1)
                .durability(1000)
                .setId(id));
    }

    // ==================== 长按机制 ====================

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isDamaged() && stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int usedTicks = MAX_USE_DURATION - remainingUseDuration;

        if (usedTicks > 0 && usedTicks % DRAIN_INTERVAL == 0) {
            if (!level.isClientSide()) {
                drainLife(player, stack);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        super.releaseUsing(stack, level, livingEntity, timeCharged);
        return false;
    }

    /**
     * 吸收周围敌人的气血之力，并发射红色激光
     */
    private void drainLife(Player player, ItemStack stack) {
        Level level = player.level();

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(DRAIN_RANGE),
                e -> e != player && e.isAlive()
        );

        if (!targets.isEmpty()) {
            LivingEntity closest = targets.stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                    .orElse(null);

            float totalHeal = 0.0F;
            for (LivingEntity target : targets) {
                // ✅ 改用间接魔法，携带玩家作为施法者
                target.hurt(player.damageSources().indirectMagic(player, player), DRAIN_DAMAGE);
                totalHeal += DRAIN_DAMAGE * HEAL_RATIO;
            }
            player.heal(totalHeal);

            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION, 60, 0, false, false, true));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        15, 0.5, 0.5, 0.5, 0.05
                );
            }

            // ✅ 向最近目标发射雷电链条
            if (closest != null && player instanceof ServerPlayer serverPlayer) {
                InteractionHand hand = player.getUsedItemHand();
                Vec3 start = getLampPosition(player, hand);

                BloodLotusLaserPayload payload = BloodLotusLaserPayload.create(
                        start, closest.getUUID(), 40);  // 持续 2 秒

                serverPlayer.level().getPlayers(p -> p.distanceTo(player) < 64)
                        .forEach(p -> ServerPlayNetworking.send(p, payload));
            }
        }

        stack.hurtAndBreak(1, player,
                player.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND);
    }

    /**
     * 获取宝莲灯在世界中的位置（用于激光起点）
     */
    private Vec3 getLampPosition(Player player, InteractionHand hand) {
        double y = player.getY() + player.getEyeHeight() - 0.4;

        float yaw = player.getYRot();
        double forwardX = -Math.sin(Math.toRadians(yaw));
        double forwardZ = Math.cos(Math.toRadians(yaw));
        double rightX = Math.cos(Math.toRadians(yaw));
        double rightZ = Math.sin(Math.toRadians(yaw));

        double x = player.getX();
        double z = player.getZ();

        if (hand == InteractionHand.MAIN_HAND) {
            x += rightX * 0.35;
            z += rightZ * 0.35;
        } else {
            x -= rightX * 0.35;
            z -= rightZ * 0.35;
        }

        x += forwardX * 0.4;
        z += forwardZ * 0.4;

        return new Vec3(x, y, z);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context,
                                TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("item.corpseorigin.blood_lotus_lantern.desc"));
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }

    // ==================== GeoItem 接口 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BloodLotusLamp>("idle_controller", 0, this::predicate)
                .triggerableAnim("idle", IDLE));
    }

    private PlayState predicate(AnimationTest<BloodLotusLamp> test) {
        return test.setAndContinue(IDLE);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BloodLotusLampRenderer renderer;

            @Override
            public @Nullable GeoItemRenderer<BloodLotusLamp> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new BloodLotusLampRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}