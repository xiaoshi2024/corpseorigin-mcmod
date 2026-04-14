package com.phagens.corpseorigin.Item;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.client.Renderer.item.HairDryerRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Consumer;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

public class HairDryerItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 动画定义
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation USE_ANIMATION = RawAnimation.begin().thenPlay("use");
    private static final RawAnimation CHARGE_ANIMATION = RawAnimation.begin().thenPlay("charge");

    // 用于追踪动画状态
    private long lastUseTime = 0;
    private long lastChargeTime = 0;

    // 存储玩家的蓄力开始时间（用于客户端粒子效果）
    private static final Map<UUID, Long> chargingPlayers = new HashMap<>();
    // 最大蓄力时间（tick）
    private static final int MAX_CHARGE_TICKS = 100; // 5秒 = 100 tick

    // 红石检测半径
    private static final int REDSTONE_CHECK_RADIUS = 5;

    public HairDryerItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
        EVENT_BUS.addListener(this::onPlayerRightClick);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<HairDryerItem> idleController = new AnimationController<>(
                this, "idleController", 0, state -> {
            state.setAnimation(IDLE_ANIMATION);
            return PlayState.CONTINUE;
        }
        );

        AnimationController<HairDryerItem> useController = new AnimationController<>(
                this, "useController", 0, state -> {
            return PlayState.STOP;
        }
        );
        useController.triggerableAnim("use", USE_ANIMATION);

        AnimationController<HairDryerItem> chargeController = new AnimationController<>(
                this, "chargeController", 0, state -> {
            if (shouldPlayChargeAnimation()) {
                state.setAnimation(CHARGE_ANIMATION);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }
        );

        controllers.add(idleController, useController, chargeController);
    }

    private boolean shouldPlayChargeAnimation() {
        return System.currentTimeMillis() - lastChargeTime < 5000;
    }

    /**
     * 检查玩家附近是否有红石能量源
     */
    private boolean hasNearbyRedstonePower(Level level, Player player) {
        BlockPos center = player.blockPosition();

        // 检查周围区域
        for (int dx = -REDSTONE_CHECK_RADIUS; dx <= REDSTONE_CHECK_RADIUS; dx++) {
            for (int dy = -REDSTONE_CHECK_RADIUS; dy <= REDSTONE_CHECK_RADIUS; dy++) {
                for (int dz = -REDSTONE_CHECK_RADIUS; dz <= REDSTONE_CHECK_RADIUS; dz++) {
                    BlockPos checkPos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    // 检查红石块
                    if (state.is(Blocks.REDSTONE_BLOCK)) {
                        return true;
                    }

                    // 检查红石火把（点燃的）
                    if (state.is(Blocks.REDSTONE_TORCH) && state.getValue(RedstoneTorchBlock.LIT)) {
                        return true;
                    }

                    // 检查拉杆（拉下的）
                    if (state.is(Blocks.LEVER) && state.getValue(LeverBlock.POWERED)) {
                        return true;
                    }

                    // 检查按钮（按下的）- 通过红石信号检测
                    if (level.getBestNeighborSignal(checkPos) > 0) {
                        return true;
                    }

                    // 检查红石线是否有信号
                    if (state.is(Blocks.REDSTONE_WIRE) && state.getValue(net.minecraft.world.level.block.RedStoneWireBlock.POWER) > 0) {
                        return true;
                    }

                    // 检查红石比较器/中继器
                    if (state.is(Blocks.COMPARATOR) && state.getValue(net.minecraft.world.level.block.ComparatorBlock.POWERED)) {
                        return true;
                    }
                    if (state.is(Blocks.REPEATER) && state.getValue(net.minecraft.world.level.block.RepeaterBlock.POWERED)) {
                        return true;
                    }

                    // 检查观察者（输出面）
                    if (state.is(Blocks.OBSERVER) && state.getValue(net.minecraft.world.level.block.ObserverBlock.POWERED)) {
                        return true;
                    }

                    // 检查红石灯（亮着的）
                    if (state.is(Blocks.REDSTONE_LAMP) && state.getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 获取使用动画（参考弓，返回 bow）
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    // 获取使用持续时间（最大蓄力时间）
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_CHARGE_TICKS;
    }

    // 检查是否受损
    private boolean isWaterDamaged(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("WaterDamaged")) {
            return customData.copyTag().getBoolean("WaterDamaged");
        }
        return false;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ===== 检查附近是否有红石能量源（优先检查，无论是否受损）=====
        boolean hasRedstone = hasNearbyRedstonePower(level, player);

        if (!hasRedstone) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 0.5F);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c需要附近有红石能量才能使用！"),
                        true
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        // ===== 有红石能量 =====

        // 检查是否是受损吹风机（进水过的）
        if (isWaterDamaged(stack)) {
            // 受损吹风机接通电源 → 短路爆炸！
            if (!level.isClientSide) {
                explodeAndDamage(level, player, stack);
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }

        // ===== 正常吹风机的逻辑 =====
        // 开始蓄力
        player.startUsingItem(hand);

        if (!level.isClientSide) {
            chargingPlayers.put(player.getUUID(), System.currentTimeMillis());

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.5F, 1.0F);

            // 触发使用动画
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level),
                    "useController", "use");
        }

        return InteractionResultHolder.consume(stack);
    }

    //让使用坏吹风的玩家燃烧
    private void explodeAndDamage(Level level, Player player, ItemStack stack) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // 创建爆炸
        level.explode(player, x, y, z, 3.0F, true, Level.ExplosionInteraction.TNT);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        x + (level.random.nextDouble() - 0.5) * 2,
                        y + level.random.nextDouble() * 2,
                        z + (level.random.nextDouble() - 0.5) * 2,
                        1, 0, 0, 0, 0.05);
            }

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    x, y + 0.5, z,
                    10, 0.5, 0.5, 0.5, 0.1);

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, y + 0.5, z,
                    20, 1.0, 1.0, 1.0, 0.2);
        }

        level.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 2.0F, 0.8F);

        level.playSound(null, x, y, z,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1.0F, 0.5F);

        player.hurt(level.damageSources().explosion(player, null), 4.0F);

        // 修复：使用 igniteForSeconds 替代 setSecondsOnFire
        player.igniteForSeconds(3);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c§l⚠ 吹风机进水损坏，爆炸了！"),
                true
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<net.minecraft.network.chat.Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (isWaterDamaged(stack)) {
            tooltipComponents.add(
                    net.minecraft.network.chat.Component.literal("§c⚠ 进水损坏 - 使用会爆炸！")
            );
        }
    }

    // 释放物品（松开右键）- 核心方法，参考弓的 releaseUsing
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player)) return;

        // 再次检查红石能量
        if (!hasNearbyRedstonePower(level, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c红石能量已消失，无法使用！"),
                        true
                );
            }
            chargingPlayers.remove(player.getUUID());
            return;
        }

        // 计算蓄力时间
        int i = this.getUseDuration(stack, livingEntity) - timeCharged;
        float chargeMultiplier = getChargeMultiplier(i);

        // 只有蓄力超过 5 tick 才触发效果
        if (chargeMultiplier >= 0.1F && !level.isClientSide) {
            int effectDuration = (int) (100 + chargeMultiplier * 200);
            int effectAmplifier = (int) (chargeMultiplier * 2);

            player.addEffect(new MobEffectInstance(MobEffects.LUCK, effectDuration, effectAmplifier));

            int durabilityCost = Math.max(1, (int) (chargeMultiplier * 3));
            stack.hurtAndBreak(durabilityCost, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));

            float volume = 0.5F + chargeMultiplier * 0.5F;
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, volume, 1.0F);

            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);

            // 释放时触发动画（表示完成）
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level),
                    "useController", "use");
        }

        chargingPlayers.remove(player.getUUID());
    }

    /**
     * 计算蓄力倍率（参考弓）
     * 蓄力时间越长，倍率越高，最高 1.0
     */
    private float getChargeMultiplier(int chargeTicks) {
        float f = (float) chargeTicks / (float) MAX_CHARGE_TICKS;
        f = (f * f + f * 2.0F) / 3.0F; // 曲线平滑增长
        if (f > 1.0F) f = 1.0F;
        if (f < 0.1F) f = 0.1F; // 最小倍率
        return f;
    }

    private void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        // 已在 use 方法中处理
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HairDryerRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new HairDryerRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // 添加蓄力粒子效果（客户端）
        if (entity instanceof Player player && isSelected && player.isUsingItem() &&
                player.getUseItem() == stack && level.isClientSide) {

            // 获取剩余使用时间
            int remainingTicks = player.getTicksUsingItem();
            int usedTicks = getUseDuration(stack, player) - remainingTicks;
            float chargeMultiplier = getChargeMultiplier(usedTicks);

            // 根据蓄力进度添加粒子效果（聚集在头部）
            addChargingParticles(player, level, chargeMultiplier);
        }

        // 手持时产生微风粒子效果（仅客户端）
        if (isSelected && level.isClientSide && entity instanceof Player && level.getGameTime() % 10 == 0) {
            level.addParticle(ParticleTypes.CLOUD,
                    entity.getX(),
                    entity.getY() + 1.2,
                    entity.getZ(),
                    (entity.getRandom().nextDouble() - 0.5) * 0.1,
                    0.05,
                    (entity.getRandom().nextDouble() - 0.5) * 0.1);
        }
    }

    /**
     * 客户端：添加蓄力粒子效果 - 聚集在玩家头部周围
     * @param chargeMultiplier 蓄力倍率 (0.1 - 1.0)
     */
    private void addChargingParticles(Player player, Level level, float chargeMultiplier) {
        // 根据蓄力倍率决定粒子数量
        int particleCount = (int) (chargeMultiplier * 12) + 2;

        // 头部中心位置
        double headX = player.getX();
        double headY = player.getY() + player.getEyeHeight() + 0.15;
        double headZ = player.getZ();

        for (int i = 0; i < particleCount; i++) {
            // 在头部周围球形分布，半径随蓄力进度增大
            double angle1 = player.getRandom().nextDouble() * Math.PI * 2;
            double angle2 = player.getRandom().nextDouble() * Math.PI * 2;
            double radius = 0.2 + chargeMultiplier * 0.5;

            double offsetX = Math.sin(angle1) * Math.cos(angle2) * radius;
            double offsetY = Math.sin(angle1) * Math.sin(angle2) * radius + 0.1;
            double offsetZ = Math.cos(angle1) * radius;

            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    headX + offsetX,
                    headY + offsetY,
                    headZ + offsetZ,
                    (player.getRandom().nextDouble() - 0.5) * 0.1,
                    player.getRandom().nextDouble() * 0.1,
                    (player.getRandom().nextDouble() - 0.5) * 0.1);
        }

        // 高蓄力时增加额外粒子效果
        if (chargeMultiplier >= 0.8F) {
            int extraParticles = (int) (chargeMultiplier * 8);
            for (int i = 0; i < extraParticles; i++) {
                double angle1 = player.getRandom().nextDouble() * Math.PI * 2;
                double angle2 = player.getRandom().nextDouble() * Math.PI * 2;
                double radius = 0.6 + player.getRandom().nextDouble() * 0.3;

                double offsetX = Math.sin(angle1) * Math.cos(angle2) * radius;
                double offsetY = Math.sin(angle1) * Math.sin(angle2) * radius + 0.2;
                double offsetZ = Math.cos(angle1) * radius;

                level.addParticle(ParticleTypes.ENCHANT,
                        headX + offsetX,
                        headY + offsetY,
                        headZ + offsetZ,
                        0, 0.05, 0);
            }
        }

        // 蓄力满时播放音效提示
        if (chargeMultiplier >= 0.99F && level.getGameTime() % 10 == 0) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 0.5F, 2.0F);
        }
    }

    /**
     * 检查附近是否有红石能量源（用于水中放电）
     */
    public boolean hasNearbyRedstonePowerForWater(Level level, BlockPos pos) {
        for (int dx = -REDSTONE_CHECK_RADIUS; dx <= REDSTONE_CHECK_RADIUS; dx++) {
            for (int dy = -REDSTONE_CHECK_RADIUS; dy <= REDSTONE_CHECK_RADIUS; dy++) {
                for (int dz = -REDSTONE_CHECK_RADIUS; dz <= REDSTONE_CHECK_RADIUS; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(Blocks.REDSTONE_BLOCK) ||
                            (state.is(Blocks.REDSTONE_TORCH) && state.getValue(RedstoneTorchBlock.LIT)) ||
                            (state.is(Blocks.LEVER) && state.getValue(LeverBlock.POWERED)) ||
                            (state.is(Blocks.REDSTONE_LAMP) && state.getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT))) {
                        return true;
                    }

                    if (level.getBestNeighborSignal(checkPos) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 处理丢入水中的电击逻辑（只负责电击伤害，不处理自身耐久）
    public void onDroppedIntoWater(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!level.isClientSide) {
            // ===== 1. 检查是否已经损坏过 =====
            if (isWaterDamaged(stack)) {
                CorpseOrigin.LOGGER.info("吹风机已损坏，跳过电击逻辑");
                return;
            }

            // ===== 2. 检查附近是否有红石能量源 =====
            boolean hasRedstone = hasNearbyRedstonePowerForWater(level, pos);

            if (!hasRedstone) {
                // 没有红石，只播放一点水花效果，不电击
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH,
                        SoundSource.BLOCKS, 0.5F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10, 0.3, 0.3, 0.3, 0);
                }
                CorpseOrigin.LOGGER.info("吹风机掉入水中但附近无红石，不触发攻击");
                return;
            }

            // ===== 3. 有红石 + 未损坏 = 触发完整电击效果 =====
            CorpseOrigin.LOGGER.info("吹风机掉入水中且附近有红石，触发攻击");

            // 记录充电时间以触发动画
            lastChargeTime = System.currentTimeMillis();

            // 检查水中的生物 - 扩大检测范围到以掉落点为中心8格内的水体
            AABB aabb = new AABB(pos).inflate(8.0D);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);

            boolean anyEntityDamaged = false;

            // 记录已伤害的实体，避免重复
            List<LivingEntity> damagedEntities = new ArrayList<>();

            for (LivingEntity livingEntity : entities) {
                // 改进：检查实体是否在水中或接触水
                boolean isInWater = livingEntity.isInWater();
                boolean isTouchingWater = isEntityTouchingWater(livingEntity, level);
                boolean isInWaterRange = isEntityInWaterRange(livingEntity, pos);

                // 只要满足任一条件就造成伤害
                if (isInWater || isTouchingWater || isInWaterRange) {
                    // 避免重复伤害同一个实体
                    if (damagedEntities.contains(livingEntity)) continue;
                    damagedEntities.add(livingEntity);
                    anyEntityDamaged = true;

                    // 有红石：高伤害 + 强效果
                    livingEntity.hurt(level.damageSources().lightningBolt(), 16.0F);
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));

                    if (level instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 15; i++) {
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                    livingEntity.getX(), livingEntity.getY() + 0.5, livingEntity.getZ(),
                                    3, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                }
            }

            // 粒子效果
            if (anyEntityDamaged) {
                level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.BLOCKS, 1.5F, 1.0F);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            30, 1.0, 1.0, 1.0, 0.2);
                }
            } else {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            20, 0.5, 0.5, 0.5, 0);
                }
                CorpseOrigin.LOGGER.info("未找到水中实体，位置: " + pos);
            }

            // 播放水花溅射音效
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * 检查实体是否在掉落点的水体范围内
     */
    private boolean isEntityInWaterRange(LivingEntity entity, BlockPos dropPos) {
        // 检查实体周围是否有水源
        BlockPos entityPos = entity.blockPosition();

        // 检查实体周围3格内的水方块
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = entityPos.offset(dx, dy, dz);
                    if (entity.level().getFluidState(checkPos).getType() == Fluids.WATER ||
                            entity.level().getFluidState(checkPos).getType() == Fluids.FLOWING_WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查实体是否接触到水（部分浸入）
     */
    private boolean isEntityTouchingWater(LivingEntity entity, Level level) {
        // 检查实体脚部位置是否在水中
        BlockPos feetPos = entity.blockPosition();
        return level.getFluidState(feetPos).getType() == Fluids.WATER ||
                level.getFluidState(feetPos).getType() == Fluids.FLOWING_WATER;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}