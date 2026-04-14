package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.api.infection.EntityInfectionRegistry;
import com.phagens.corpseorigin.api.infection.InfectionAPI;
import com.phagens.corpseorigin.data.InfectionData;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 尸体系统处理器
 *
 * 【功能说明】
 * 完全参照 Mob-Dismemberment 1.12.2 的设计
 * 1. 生物死亡时创建残肢实体 (EntityGib)
 * 2. 只有 type 3 (身体/body) 可以被感染变成僵尸
 * 3. 感染来源：龙右或尸水
 *
 * 【残肢类型】
 * 0 = 头部 (head)
 * 1 = 左臂 (left arm)
 * 2 = 右臂 (right arm)
 * 3 = 身体 (body) - 可被感染
 * 4 = 左腿 (left leg)
 * 5 = 右腿 (right leg)
 * 6+ = 苦力怕脚 (creeper feet)
 *
 * 【参考原作】
 * - 邪罗汉被砍头后尸体变成尸兄
 * - 尔多被打成一坨后也变成了尸兄
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class CorpseInfectionHandler {

    /**
     * 生物死亡时创建残肢
     * 完全参照 Mob-Dismemberment 的 EntityGib 创建逻辑
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        // 只在服务器端执行
        if (level.isClientSide) return;

        // 不创建玩家残肢（玩家有特殊处理）
        if (entity instanceof Player) return;

        // 不创建尸兄的残肢
        if (entity.getType() == EntityRegistry.LOWER_LEVEL_ZB.get() ||
            entity.getType() == EntityRegistry.LONGYOU.get() ||
            entity.getType() == EntityRegistry.GUIGUN.get() ||
            entity.getType() == EntityRegistry.ZBR_FISH.get()) {
            return;
        }

        // 检查是否应该生成残肢
        if (!shouldCreateGibs(entity)) {
            return;
        }

        // 检查死亡原因是否是爆炸
        boolean isExplosion = event.getSource() != null &&
                (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) ||
                        event.getSource().getEntity() instanceof Creeper);

        // 生成残肢
        createGibs(entity, isExplosion, event);
    }

    /**
     * 检查是否应该为该生物生成残肢
     * 使用新的API系统，支持外部模组注册
     */
    private static boolean shouldCreateGibs(LivingEntity entity) {
        // 不生成残肢的生物类型
        String entityType = entity.getType().toString().toLowerCase();
        if (entityType.contains("corpse_gib") ||
                entity.isBaby()) {
            return false;
        }

        // 检查是否通过API注册了感染
        if (InfectionAPI.shouldCreateCorpse(entity)) {
            return true;
        }

        // 检查是否通过实体注册系统注册了感染
        if (EntityInfectionRegistry.shouldCreateCorpse(entity.getType())) {
            return true;
        }

        // 默认：只有僵尸和村民死亡时才会生成尸体
        return isZombieOrVillager(entity);
    }

    /**
     * 检查是否是僵尸或村民
     */
    private static boolean isZombieOrVillager(LivingEntity entity) {
        // 检查是否是僵尸类生物
        if (entity instanceof net.minecraft.world.entity.monster.Zombie) {
            return true;
        }
        // 检查是否是村民
        if (entity instanceof net.minecraft.world.entity.npc.Villager) {
            return true;
        }
        // 检查实体类型名称
        String entityType = entity.getType().toString().toLowerCase();
        return entityType.contains("zombie") || entityType.contains("villager");
    }

    /**
     * 创建残肢
     * 参照 Mob-Dismemberment 的 EntityGib 创建逻辑
     */
    private static void createGibs(LivingEntity entity, boolean isExplosion, LivingDeathEvent event) {
        Level level = entity.level();
        boolean isHuman = isHumanEntity(entity);
        boolean isStrong = isStrongEntity(entity);
        boolean isCreeper = entity instanceof Creeper;

        // 确定要生成的残肢类型
        int[] gibTypes;

        if (isExplosion) {
            // 爆炸时必定掉落更多残肢
            if (isCreeper) {
                // 苦力怕掉落脚
                gibTypes = new int[]{0, 3, 6, 7, 8, 9}; // 头、身体、4只脚
            } else {
                gibTypes = new int[]{0, 1, 2, 3, 4, 5}; // 所有部件
            }
        } else {
            // 普通死亡随机掉落
            if (isHuman || isStrong) {
                // 人类和强者掉落身体（可被感染）和其他部件
                gibTypes = new int[]{0, 3}; // 头和身体
            } else {
                // 其他生物随机掉落
                int count = entity.getRandom().nextInt(3);
                gibTypes = new int[count];
                for (int i = 0; i < count; i++) {
                    gibTypes[i] = entity.getRandom().nextInt(6);
                }
            }
        }

        // 创建残肢实体
        for (int gibType : gibTypes) {
            CorpseGibEntity gib = CorpseGibEntity.create(level, entity, gibType,
                    isExplosion ? event.getSource().getEntity() : null);
            level.addFreshEntity(gib);
        }

        if (gibTypes.length > 0) {
//            CorpseOrigin.LOGGER.info("生成 {} 个残肢来自 {} (人类: {}, 强者: {}, 爆炸: {})",
//                    gibTypes.length, entity.getName().getString(), isHuman, isStrong, isExplosion);
        }
    }

    /**
     * 判断是否为人类实体
     */
    private static boolean isHumanEntity(LivingEntity entity) {
        String type = entity.getType().toString().toLowerCase();

        return entity instanceof Player ||
                entity instanceof net.minecraft.world.entity.npc.Villager ||
                type.contains("player") ||
                type.contains("villager") ||
                type.contains("wandering_trader") ||
                type.contains("pillager") ||
                type.contains("vindicator") ||
                type.contains("evoker") ||
                type.contains("witch");
    }

    /**
     * 判断是否为强者实体
     */
    private static boolean isStrongEntity(LivingEntity entity) {
        // 高血量生物
        if (entity.getMaxHealth() >= 50) return true;

        // 精英怪
        String type = entity.getType().toString().toLowerCase();
        return type.contains("warden") ||
                type.contains("wither") ||
                type.contains("ender_dragon") ||
                type.contains("ravager") ||
                type.contains("elder_guardian") ||
                type.contains("iron_golem");
    }

    /**
     * 每tick检查身体残肢的感染
     * 只有 type 3 (body) 可以被感染
     */
    private static final int CHECK_INTERVAL = 40; // 每2秒检查一次
    private static List<CorpseGibEntity> lastBodies = new ArrayList<>();
    private static long lastCheckTime = 0;
    
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        // 每40tick检查一次（每2秒）
        if (level.getGameTime() % CHECK_INTERVAL != 0) return;

        // 获取所有身体残肢实体 (type 3)
        List<CorpseGibEntity> bodies = level.getEntitiesOfClass(
                CorpseGibEntity.class,
                new AABB(
                        level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(),
                        level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ()
                ),
                entity -> entity.getGibType() == CorpseGibEntity.GIB_TYPE_BODY
        );

        // 限制检查的残肢数量，避免过多实体导致性能问题
        int maxBodiesToCheck = 50;
        if (bodies.size() > maxBodiesToCheck) {
            // 随机选择一部分残肢进行检查
            Collections.shuffle(bodies);
            bodies = bodies.subList(0, maxBodiesToCheck);
        }

        for (CorpseGibEntity body : bodies) {
            // 检查是否接触尸水
            if (isInCorpseWater(level, body)) {
                // 增加感染进度（每2秒增加 10%）
                float oldProgress = body.getInfectionProgress();
                if (oldProgress < 1.0f) {
                    body.addInfectionProgress(0.10f);
                    float newProgress = body.getInfectionProgress();

                    if (oldProgress < 1.0f && newProgress >= 1.0f) {
                        CorpseOrigin.LOGGER.info("身体残肢 {} 被尸水完全感染，即将变成僵尸！",
                                body.getParentType());
                    }
                }
            }

            // 检查是否靠近龙右（尸王可以主动感染尸体）
            if (isNearLongyou(level, body)) {
                // 龙右感染更快（每2秒增加 40%）
                float oldProgress = body.getInfectionProgress();
                if (oldProgress < 1.0f) {
                    body.addInfectionProgress(0.40f);
                    float newProgress = body.getInfectionProgress();

                    if (oldProgress < 1.0f && newProgress >= 1.0f) {
                        CorpseOrigin.LOGGER.info("身体残肢 {} 被龙右完全感染，即将变成僵尸！",
                                body.getParentType());
                    }
                }
            }
        }
        
        lastBodies = bodies;
        lastCheckTime = level.getGameTime();
    }

    /**
     * 检查残肢是否靠近龙右（尸王可以主动感染尸体）
     */
    private static boolean isNearLongyou(Level level, CorpseGibEntity gib) {
        // 查找附近的龙右实体
        var longyouList = level.getEntitiesOfClass(
                com.phagens.corpseorigin.entity.LongyouEntity.class,
                gib.getBoundingBox().inflate(5.0D), // 5格范围内
                entity -> entity.isAlive()
        );

        return !longyouList.isEmpty();
    }

    /**
     * 检查残肢是否接触尸水
     */
    private static boolean isInCorpseWater(Level level, CorpseGibEntity gib) {
        BlockPos pos = gib.blockPosition();
        AABB boundingBox = gib.getBoundingBox();

        // 检查残肢周围的方块
        int minX = (int) Math.floor(boundingBox.minX);
        int minY = (int) Math.floor(boundingBox.minY);
        int minZ = (int) Math.floor(boundingBox.minZ);
        int maxX = (int) Math.ceil(boundingBox.maxX);
        int maxY = (int) Math.ceil(boundingBox.maxY);
        int maxZ = (int) Math.ceil(boundingBox.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    // 检查是否是水且被感染
                    if (state.getFluidState().is(FluidTags.WATER)) {
                        if (level instanceof ServerLevel serverLevel) {
                            if (InfectionData.isWaterInfectedStatic(serverLevel, checkPos)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
