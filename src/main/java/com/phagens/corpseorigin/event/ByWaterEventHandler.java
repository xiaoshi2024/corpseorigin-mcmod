/**
 * 被污染的水源事件处理器 - 处理玩家接触感染水源时的中毒效果
 *
 * 【功能说明】
 * 1. 检测玩家是否接触到被七星棺感染的水源
 * 2. 根据水源能量值决定是否施加中毒效果（玩家）
 * 3. 支持水源感染的扩散（放置水源时检查相邻感染水源）
 * 4. 冷却机制防止效果频繁触发
 *
 * 【注意】
 * 村民不再通过接触尸水直接获得变异buff
 * 村民感染改为通过尸体感染系统实现（参见 CorpseInfectionHandler）
 * 或龙右的主动感染能力
 *
 * 【工作原理】
 * - 每500毫秒检查一次玩家位置
 * - 检查实体所在方块是否为感染水源
 * - 满足条件则施加对应效果
 * - 中毒效果有3000毫秒冷却时间
 *
 * 【感染条件】
 * 1. 实体所在位置的水源被感染（InfectionData中记录）
 * 2. 水源能量值 > 0
 * 3. 通过冷却时间检查
 *
 * 【关联系统】
 * - InfectionData: 水源感染数据存储
 * - MobEffects.POISON: 玩家中毒效果
 * - BlockEvent.EntityPlaceEvent: 水源放置事件
 * - CorpseInfectionHandler: 尸体感染系统（村民感染新途径）
 *
 * @author Phagens
 * @version 1.1
 */
package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.data.InfectionData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class ByWaterEventHandler {
    /** 玩家中毒冷却映射表 - 记录每个玩家上次中毒时间 */
    private static final Map<UUID, Long> playerPoisonCooldowns = new HashMap<>();
    /** 检查冷却映射表 - 记录每个实体上次检查时间 */
    private static final Map<UUID, Long> playerCheckCooldowns = new HashMap<>();
    /** 中毒效果冷却时间（毫秒） */
    private static final long POISON_COOLDOWN = 3000;
    /** 中毒效果持续时间（tick） */
    private static final int POISON_DURATION = 60;
    /** 中毒效果等级 */
    private static final int POISON_AMPLIFIER = 0;
    /** 位置检查间隔（毫秒） */
    private static final long CHECK_INTERVAL = 500;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide) {
            return;
        }

        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastCheckTime = playerCheckCooldowns.get(playerUUID);

        if (lastCheckTime == null || (currentTime - lastCheckTime) >= CHECK_INTERVAL) {
            BlockPos playerPos = player.blockPosition();
            
            if (isPlayerInInfectedWater(level, playerPos)) {
                applyPoisonEffect(player);
            }
            
            playerCheckCooldowns.put(playerUUID, currentTime);
        }
    }

    // 村民接触尸水不再直接获得变异buff，改为通过尸体感染系统实现
    // 参见 CorpseInfectionHandler 和龙右感染机制


    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player && event.getLevel() instanceof ServerLevel serverLevel) {
            BlockState state = event.getPlacedBlock();
            if (state.getFluidState().is(FluidTags.WATER)) {
                BlockPos pos = event.getPos();
                if (isNextToInfectedWater(serverLevel, pos)) {
                    InfectionData.markWaterInfectedStatic(serverLevel, pos);
                }
            }
        }

    }

    private static boolean isPlayerInInfectedWater(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getFluidState().is(FluidTags.WATER)) {
            if (level instanceof ServerLevel serverLevel) {
                return InfectionData.isWaterInfectedStatic(serverLevel, pos);
            }
        }
        return false;
    }

    private static boolean isNextToInfectedWater(ServerLevel level, BlockPos pos) {
        // 限制同化范围为1格（直接相邻）
        final int MAX_RANGE = 1;
        
        for (int x = -MAX_RANGE; x <= MAX_RANGE; x++) {
            for (int y = -MAX_RANGE; y <= MAX_RANGE; y++) {
                for (int z = -MAX_RANGE; z <= MAX_RANGE; z++) {
                    // 跳过自身
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    // 计算曼哈顿距离，确保在范围内
                    int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
                    if (distance <= MAX_RANGE) {
                        BlockPos neighbor = pos.offset(x, y, z);
                        if (InfectionData.isWaterInfectedStatic(level, neighbor)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void applyPoisonEffect(Player player) {
        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastPoisonTime = playerPoisonCooldowns.get(playerUUID);

        if (lastPoisonTime == null || (currentTime - lastPoisonTime) >= POISON_COOLDOWN) {
            player.addEffect(new MobEffectInstance(
                MobEffects.POISON,
                POISON_DURATION,
                POISON_AMPLIFIER
            ));
            playerPoisonCooldowns.put(playerUUID, currentTime);
        }
    }
}