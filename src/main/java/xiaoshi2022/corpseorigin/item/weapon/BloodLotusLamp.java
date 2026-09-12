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
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import xiaoshi2022.corpseorigin.network.BloodLotusAuraPayload;
import xiaoshi2022.corpseorigin.network.BloodLotusLaserMultiPayload;
import xiaoshi2022.corpseorigin.registry.ModDataComponents;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
    private static final double DRAIN_RANGE = 16.0;
    /** 每次吸血伤害 */
    private static final float DRAIN_DAMAGE = 20.0F;
    /** 血气储存上限 */
    private static final int MAX_STORED_BLOOD_QI = 200;
    /** 每 tick 回血消耗的血气 */
    private static final int HEAL_COST_PER_TICK = 1;
    /** 每 tick 回血量 */
    private static final float HEAL_PER_TICK = 1.0F;
    /** 每隔多少 tick 触发一次吸血 */
    private static final int DRAIN_INTERVAL = 10;
    /** 最长长按时间 */
    private static final int MAX_USE_DURATION = 300;

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

        // ✅ 潜行 + 右键 = 释放血气（自身环绕）
        if (player.isShiftKeyDown()) {
            int stored = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
            if (stored > 0 && !level.isClientSide()) {
                // 回血
                player.heal(stored);
                stack.set(ModDataComponents.STORED_BLOOD_QI, 0);

                // ✅ 延长 buff：抗性提升 + 力量 + 速度 + 生命恢复
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,   200, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED,      200, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));

                // ✅ 广播「血气包裹自身」特效
                if (player instanceof ServerPlayer serverPlayer) {
                    BloodLotusAuraPayload payload = new BloodLotusAuraPayload(
                            serverPlayer.getUUID(), 100);  // 持续 5 秒

                    serverPlayer.level().getPlayers(p -> p.distanceTo(player) < 64)
                            .forEach(p -> ServerPlayNetworking.send(p, payload));
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 长按吸血
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
                e -> e != player && e.isAlive() && !(e instanceof Player)
        );

        if (targets.isEmpty()) {
            stack.hurtAndBreak(1, player,
                    player.getUsedItemHand() == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return;
        }

        targets.sort(Comparator.comparingDouble(a -> a.distanceToSqr(player)));

        int bloodGained = 0;
        int killed = 0;

        for (LivingEntity target : targets) {
            float healthBefore = target.getHealth();

            target.hurt(player.damageSources().playerAttack(player), DRAIN_DAMAGE);

            float healthAfter = target.getHealth();
            float actualDamage = Math.max(0, healthBefore - healthAfter);

            if (actualDamage > 0) {
                bloodGained += (int) actualDamage;
            }

            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 20, 4, false, false, false));

            if (!target.isAlive()) killed++;
        }

        // ✅ 存进 DataComponent（有上限）
        if (bloodGained > 0) {
            int current = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
            int newValue = Math.min(current + bloodGained, MAX_STORED_BLOOD_QI);
            stack.set(ModDataComponents.STORED_BLOOD_QI, newValue);
        }

        // ✅ 视觉
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.8, 0.8, 0.8, 0.1
            );
            for (LivingEntity target : targets) {
                serverLevel.sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        5, 0.3, 0.3, 0.3, 0.05
                );
            }
        }

        // ✅ 激光
        LivingEntity closest = targets.get(0);
        // ✅ 合并成一个包，携带所有目标 UUID
        if (player instanceof ServerPlayer serverPlayer) {
            InteractionHand hand = player.getUsedItemHand();
            Vec3 start = getLampPosition(player, hand);

            List<UUID> targetUuids = targets.stream()
                    .map(LivingEntity::getUUID)
                    .toList();

            BloodLotusLaserMultiPayload payload = BloodLotusLaserMultiPayload.create(
                    start, targetUuids, 20);

            serverPlayer.level().getPlayers(p -> p.distanceTo(player) < 64)
                    .forEach(p -> ServerPlayNetworking.send(p, payload));
        }

        // ✅ 耐久消耗
        int durabilityCost = 1 + killed;
        stack.hurtAndBreak(durabilityCost, player,
                player.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof ServerPlayer player)) return;

        // ✅ 只在主手/副手持有的时候回血
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;

        int stored = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
        if (stored <= 0) return;

        // ✅ 玩家血量不满才回
        if (player.getHealth() >= player.getMaxHealth()) return;

        // ✅ 消耗储存，回血
        int cost = Math.min(HEAL_COST_PER_TICK, stored);
        stack.set(ModDataComponents.STORED_BLOOD_QI, stored - cost);

        float healAmount = cost * HEAL_PER_TICK;
        player.heal(healAmount);

        // ✅ 可选：回血时冒红光
        if (level.getGameTime() % 10 == 0) {
            level.sendParticles(
                    ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    1, 0.3, 0.3, 0.3, 0.0
            );
        }
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
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        int stored = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
        int max = MAX_STORED_BLOOD_QI;

        builder.accept(Component.translatable("item.corpseorigin.blood_lotus_lantern.desc"));
        builder.accept(Component.translatable("item.corpseorigin.blood_lotus_lantern.blood_qi",
                stored, max).withStyle(ChatFormatting.RED));

        super.appendHoverText(stack, context, display, builder, flag);
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