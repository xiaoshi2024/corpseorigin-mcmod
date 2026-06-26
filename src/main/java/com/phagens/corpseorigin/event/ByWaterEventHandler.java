/**
 * 被污染的水源事件处理器 - 处理玩家接触感染水源时的中毒效果
 *
 * 【功能说明】
 * 1. 检测玩家是否接触到被七星棺感染的水源
 * 2. 检测玩家是否在死寂群系（尸兄群系）中接触水源
 * 3. 根据水源状态决定是否施加中毒效果（玩家）
 * 4. 冷却机制防止效果频繁触发
 *
 * 【注意】
 * 村民不再通过接触尸水直接获得变异buff
 * 村民感染改为通过尸体感染系统实现（参见 CorpseInfectionHandler）
 * 或龙右的主动感染能力
 *
 * 【工作原理】
 * - 每500毫秒检查一次玩家位置
 * - 检查实体所在方块是否为感染水源 或 是否在死寂群系中
 * - 满足条件则施加对应效果
 * - 中毒效果有3000毫秒冷却时间
 *
 * 【感染条件】（满足任一即可）
 * 1. 水源被感染（InfectionData中记录）
 * 2. 玩家所在位置的群系是死寂群系
 * 3. 通过冷却时间检查
 *
 * 【关联系统】
 * - InfectionData: 水源感染数据存储（棺材感染）
 * - BiomeRegistry: 死寂群系注册
 * - MobEffects.POISON: 玩家中毒效果
 * - CorpseInfectionHandler: 尸体感染系统（村民感染新途径）
 *
 * @author Phagens
 * @version 2.0
 */
package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.data.InfectionData;
import com.phagens.corpseorigin.register.BiomeRegistry;
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

    /**
     * 检查玩家是否在感染水中
     * 判断条件：（满足任一即可）
     * 1. 水源被七星棺感染（InfectionData中记录）
     * 2. 玩家所在位置的群系是死寂群系
     * 
     * @param level 世界
     * @param pos 玩家位置
     * @return 是否在感染水中
     */
    private static boolean isPlayerInInfectedWater(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getFluidState().is(FluidTags.WATER)) {
            if (level instanceof ServerLevel serverLevel) {
                // 检查条件1：水源被感染（InfectionData记录）
                boolean isWaterInfected = InfectionData.isWaterInfectedStatic(serverLevel, pos);
                
                // 检查条件2：玩家所在群系是死寂群系
                boolean isInCorpseBiome = serverLevel.getBiome(pos).is(BiomeRegistry.DEAD_SILENCE);
                
                // 满足任一条件即为感染水
                return isWaterInfected || isInCorpseBiome;
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