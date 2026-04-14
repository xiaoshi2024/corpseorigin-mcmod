package com.phagens.corpseorigin.event.item;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.HairDryerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class HairDryerEventHandler {

    private static final Map<ItemEntity, Long> itemsToCheck = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    // 耐久减少量范围
    private static final int MIN_DAMAGE = 30;
    private static final int MAX_DAMAGE = 80;

    // 检测间隔（tick）
    private static final int CHECK_INTERVAL = 10;

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();

        if (stack.getItem() instanceof HairDryerItem) {
            long checkTick = (long) event.getEntity().tickCount + 5;
            itemsToCheck.put(event.getEntity(), checkTick);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        List<ItemEntity> toRemove = new ArrayList<>();

        for (Map.Entry<ItemEntity, Long> entry : itemsToCheck.entrySet()) {
            ItemEntity itemEntity = entry.getKey();
            long checkTime = entry.getValue();

            if (itemEntity.tickCount >= checkTime) {
                if (itemEntity.isAlive()) {
                    BlockPos pos = itemEntity.blockPosition();

                    // 检查周围3x3x3范围内是否有水体
                    boolean isNearWater = false;
                    outer:
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                BlockPos checkPos = pos.offset(dx, dy, dz);
                                if (level.getFluidState(checkPos).getType() == Fluids.WATER ||
                                        level.getFluidState(checkPos).getType() == Fluids.FLOWING_WATER) {
                                    isNearWater = true;
                                    break outer;
                                }
                            }
                        }
                    }

                    if (isNearWater) {
                        // 处理水中的吹风机
                        ItemStack stack = itemEntity.getItem();
                        if (stack.getItem() instanceof HairDryerItem hairDryer) {
                            // 检查是否已经损坏
                            boolean alreadyDamaged = isWaterDamaged(stack);

                            if (!alreadyDamaged) {
                                // 1. 触发水中电击伤害（传递 stack 和 itemEntity）
                                hairDryer.onDroppedIntoWater(level, pos, null, stack);

                                // 2. 随机损坏或减少耐久
                                boolean directDestroy = RANDOM.nextBoolean();

                                if (directDestroy) {
                                    // 直接损坏消失
                                    itemEntity.discard();
                                    toRemove.add(itemEntity);

                                    level.playSound(null, pos,
                                            SoundEvents.GENERIC_EXTINGUISH_FIRE,
                                            SoundSource.BLOCKS, 1.0F, 0.5F);

                                    if (level instanceof ServerLevel serverLevel) {
                                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                15, 0.3, 0.3, 0.3, 0.1);
                                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                10, 0.5, 0.5, 0.5, 0.1);
                                    }
                                } else {
                                    // 大幅减少耐久
                                    int damageAmount = MIN_DAMAGE + RANDOM.nextInt(MAX_DAMAGE - MIN_DAMAGE + 1);
                                    int currentDamage = stack.getDamageValue();
                                    int newDamage = currentDamage + damageAmount;

                                    if (newDamage >= stack.getMaxDamage()) {
                                        itemEntity.discard();
                                        toRemove.add(itemEntity);

                                        level.playSound(null, pos,
                                                SoundEvents.GENERIC_EXTINGUISH_FIRE,
                                                SoundSource.BLOCKS, 1.2F, 0.3F);

                                        if (level instanceof ServerLevel serverLevel) {
                                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                    20, 0.4, 0.4, 0.4, 0.1);
                                        }
                                    } else {
                                        stack.setDamageValue(newDamage);

                                        // 添加受损标记
                                        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                                        if (customData == null) {
                                            customData = CustomData.EMPTY;
                                        }
                                        var tag = customData.copyTag();
                                        tag.putBoolean("WaterDamaged", true);
                                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                                        level.playSound(null, pos,
                                                SoundEvents.GENERIC_SPLASH,
                                                SoundSource.BLOCKS, 0.8F, 1.2F);

                                        if (level instanceof ServerLevel serverLevel) {
                                            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                    15, 0.3, 0.3, 0.3, 0.05);
                                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                    5, 0.3, 0.3, 0.3, 0.05);
                                        }

                                        // 继续监控
                                        entry.setValue((long) (itemEntity.tickCount + CHECK_INTERVAL));
                                        continue;
                                    }
                                }
                            } else {
                                // 已经损坏过的吹风机，直接移除监控（不再处理）
                                CorpseOrigin.LOGGER.info("吹风机已损坏，移除监控");
                                toRemove.add(itemEntity);
                                continue;
                            }
                        }
                    } else {
                        // 离开水体，停止监控
                        toRemove.add(itemEntity);
                    }
                } else {
                    // 物品已不存在，移除监控
                    toRemove.add(itemEntity);
                }
            }
        }

        // 移除不需要监控的物品
        for (ItemEntity itemEntity : toRemove) {
            itemsToCheck.remove(itemEntity);
        }
    }

    // 检查是否已受损
    private static boolean isWaterDamaged(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("WaterDamaged")) {
            return customData.copyTag().getBoolean("WaterDamaged");
        }
        return false;
    }
}