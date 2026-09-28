package xiaoshi2022.corpseorigin.item.weapon;

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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
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

public class BloodLotusLamp extends Item implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final double DRAIN_RANGE = 16.0;
    private static final float DRAIN_DAMAGE = 20.0F;
    private static final int MAX_STORED_BLOOD_QI = 200;
    private static final int HEAL_COST_PER_TICK = 1;
    private static final float HEAL_PER_TICK = 1.0F;
    private static final int DRAIN_INTERVAL = 10;
    private static final int MAX_USE_DURATION = 300;

    /** 攻击触发的小范围吸血 */
    private static final double MELEE_DRAIN_RANGE = 4.0;
    private static final float MELEE_DRAIN_DAMAGE = 5.0F;

    public BloodLotusLamp(Properties properties) {
        super(properties);
    }

    public static BloodLotusLamp create(ResourceKey<Item> id) {
        return new BloodLotusLamp(new Item.Properties()
                .stacksTo(1)
                .durability(1000)
                .setId(id));
    }

    // ==================== 长按机制（仅玩家） ====================

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 潜行 + 右键 = 释放血气
        if (player.isShiftKeyDown()) {
            int stored = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
            if (stored > 0 && !level.isClientSide()) {
                level.playSound(null,player.getX(),player.getY(),player.getZ(),xiaoshi2022.corpseorigin.registry.ModSounds.BLOOD_LOTUS_DRAIN,net.minecraft.sounds.SoundSource.PLAYERS,.8f,.8f);
                player.heal(stored);
                stack.set(ModDataComponents.STORED_BLOOD_QI, 0);

                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,   200, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED,      200, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));

                if (player instanceof ServerPlayer serverPlayer) {
                    BloodLotusAuraPayload payload = new BloodLotusAuraPayload(
                            serverPlayer.getUUID(), 100);
                    serverPlayer.level().getPlayers(p -> p.distanceTo(player) < 64)
                            .forEach(p -> ServerPlayNetworking.send(p, payload));
                }
            }
            return InteractionResult.SUCCESS;
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
        if (!(livingEntity instanceof Player player)) return;

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

    // ==================== 攻击触发吸血（玩家+生物） ====================

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (level.isClientSide()) return;

        // ✅ 只打「被攻击的目标」，不扫范围
        if (attacker instanceof Player player) {
            target.hurt(player.damageSources().playerAttack(player), MELEE_DRAIN_DAMAGE);
        } else {
            target.hurt(attacker.damageSources().mobAttack(attacker), MELEE_DRAIN_DAMAGE);
        }

        int bloodGained = (int) MELEE_DRAIN_DAMAGE;

        // 吸血归属
        if (attacker instanceof Player player) {
            int current = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
            stack.set(ModDataComponents.STORED_BLOOD_QI,
                    Math.min(current + bloodGained, MAX_STORED_BLOOD_QI));
        } else {
            attacker.heal(bloodGained * 0.5F);
        }

        // ✅ 只对攻击目标发射激光
        List<LivingEntity> singleTarget = List.of(target);
        broadcastLaser(attacker, singleTarget);
    }

    // ==================== 消耗储存回血（玩家+生物） ====================

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof LivingEntity living)) return;
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;

        int stored = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
        if (stored <= 0) return;
        if (living.getHealth() >= living.getMaxHealth()) return;

        int cost = Math.min(HEAL_COST_PER_TICK, stored);
        stack.set(ModDataComponents.STORED_BLOOD_QI, stored - cost);
        living.heal(cost * HEAL_PER_TICK);

        if (living instanceof Player && level.getGameTime() % 10 == 0) {
            xiaoshi2022.corpseorigin.skill.chapter.QiEffects.aura(living,"lotus_heal",0xdd6688,1.5f,16);
        }
    }

    // ==================== 核心逻辑 ====================

    /**
     * 长按群吸（只给玩家用）
     */
    public void drainLife(LivingEntity player, ItemStack stack) {
        Level level = player.level();

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(DRAIN_RANGE),
                e -> player instanceof xiaoshi2022.corpseorigin.entity.CloneAvatarEntity
                        ? xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.canHit(player, e) && player.hasLineOfSight(e)
                        : e != player && e.isAlive() && !(e instanceof Player)
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

            target.hurt(player instanceof Player owner ? player.damageSources().playerAttack(owner)
                    : player.damageSources().mobAttack(player), DRAIN_DAMAGE);

            float healthAfter = target.getHealth();
            float actualDamage = Math.max(0, healthBefore - healthAfter);
            if (actualDamage > 0) bloodGained += (int) actualDamage;

            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 20, 4, false, false, false));

            if (!target.isAlive()) killed++;
        }

        // 存血气
        if (bloodGained > 0) {
            int current = stack.getOrDefault(ModDataComponents.STORED_BLOOD_QI, 0);
            int newValue = Math.min(current + bloodGained, MAX_STORED_BLOOD_QI);
            stack.set(ModDataComponents.STORED_BLOOD_QI, newValue);
        }

        // 视觉
        if (level instanceof ServerLevel serverLevel) {
            xiaoshi2022.corpseorigin.skill.chapter.QiEffects.aura(player,"lotus_drain",0xcc184f,2,16);
            for (LivingEntity target : targets) {
                xiaoshi2022.corpseorigin.skill.chapter.QiEffects.aura(target,"lotus_drain_target",0x991d42,1.3f,16);
            }
        }

        // 发射激光
        broadcastLaser(player, targets);

        // 耐久消耗
        int durabilityCost = 1 + killed;
        stack.hurtAndBreak(durabilityCost, player,
                player.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    /**
     * ✅ 广播激光链条（玩家和生物共用）
     */
    private void broadcastLaser(LivingEntity attacker, List<LivingEntity> targets) {
        Level level = attacker.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 计算起点
        Vec3 start;
        if (attacker instanceof Player player) {
            // 玩家用精确手部位置
            InteractionHand hand = player.getUsedItemHand();
            start = getLampPosition(player, hand);
        } else {
            // 生物用眼睛高度
            start = new Vec3(
                    attacker.getX(),
                    attacker.getY() + attacker.getEyeHeight() - 0.4,
                    attacker.getZ()
            );
        }

        List<UUID> targetUuids = targets.stream()
                .map(LivingEntity::getUUID)
                .toList();

        BloodLotusLaserMultiPayload payload = BloodLotusLaserMultiPayload.create(
                start, targetUuids, 20);

        // ✅ 广播给附近所有玩家（包括生物附近的玩家）
        serverLevel.getPlayers(p -> p.distanceTo(attacker) < 64)
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    /**
     * 获取玩家宝莲灯位置（用于激光起点）
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
