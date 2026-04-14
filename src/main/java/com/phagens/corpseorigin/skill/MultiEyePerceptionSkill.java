package com.phagens.corpseorigin.skill;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.Organic.OrdinaryZbEyeItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多眼感知技能
 * 多眼形态专属主动技能：触发时能快速感知到周围的敌意生物
 * 模拟多眼360度视角
 */
public class MultiEyePerceptionSkill extends BaseSkill {

    // 技能冷却时间（tick）- 10秒
    private static final int COOLDOWN_TICKS = 200;
    // 技能持续时间（tick）- 5秒
    private static final int DURATION_TICKS = 100;
    // 感知范围
    private static final double DETECTION_RANGE = 32.0;
    // 高亮范围
    private static final double HIGHLIGHT_RANGE = 24.0;

    // 玩家冷却记录
    private static final Map<UUID, Long> cooldownMap = new HashMap<>();
    // 玩家技能激活记录
    private static final Map<UUID, Long> activeMap = new HashMap<>();

    public MultiEyePerceptionSkill() {
        super(new Builder(id("multi_eye_perception"))
                .name(Component.translatable("skill.corpseorigin.multi_eye_perception"))
                .description(Component.translatable("skill.corpseorigin.multi_eye_perception.desc"))
                .icon(ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/skills/multi_eye_perception.png"))
                .cost(0) // 进化获得，不需要消耗进化点
                .skillType(SkillType.SPECIAL_MUTATION)
                .requiredLevel(1)
                .passive(false) // 主动技能
                .cooldown(COOLDOWN_TICKS)
                .duration(DURATION_TICKS)
        );
    }

    @Override
    public boolean canLearn(Player player) {
        // 只有进化出多眼形态的玩家才能拥有此技能
        return OrdinaryZbEyeItem.getExtraEyeCount(player) > 0;
    }

    @Override
    public void onLearn(Player player) {
        CorpseOrigin.LOGGER.info("玩家 {} 获得了多眼感知技能", player.getName().getString());
    }

    @Override
    public void onForget(Player player) {
        // 清除冷却记录
        cooldownMap.remove(player.getUUID());
        activeMap.remove(player.getUUID());
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        UUID playerId = player.getUUID();
        long currentTime = player.level().getGameTime();

        // 检查冷却
        if (cooldownMap.containsKey(playerId)) {
            long lastUseTime = cooldownMap.get(playerId);
            if (currentTime - lastUseTime < COOLDOWN_TICKS) {
                long remainingTicks = COOLDOWN_TICKS - (currentTime - lastUseTime);
                player.displayClientMessage(
                    Component.translatable("skill.corpseorigin.multi_eye.cooldown", remainingTicks / 20)
                        .withStyle(net.minecraft.ChatFormatting.RED),
                    true
                );
                return;
            }
        }

        // 激活技能
        activateSkill(player);
    }

    /**
     * 激活技能
     */
    private void activateSkill(Player player) {
        UUID playerId = player.getUUID();
        long currentTime = player.level().getGameTime();

        cooldownMap.put(playerId, currentTime);
        activeMap.put(playerId, currentTime);

        // 发送激活消息到屏幕
        player.displayClientMessage(
            Component.translatable("skill.corpseorigin.multi_eye.activated")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD),
            true
        );

        // 播放音效
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(),
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5f,
                1.5f
            );
        }

        // 给予短暂的速度和跳跃提升效果
        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED,
            DURATION_TICKS,
            1,
            false,
            false,
            true
        ));

        player.addEffect(new MobEffectInstance(
            MobEffects.JUMP,
            DURATION_TICKS,
            0,
            false,
            false,
            true
        ));

        CorpseOrigin.LOGGER.info("玩家 {} 激活了多眼感知技能", player.getName().getString());
    }

    /**
     * 更新技能效果（每tick调用）
     */
    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        UUID playerId = player.getUUID();
        long currentTime = player.level().getGameTime();

        // 检查技能是否激活
        if (!activeMap.containsKey(playerId)) {
            return;
        }

        long activationTime = activeMap.get(playerId);
        if (currentTime - activationTime > DURATION_TICKS) {
            // 技能结束
            activeMap.remove(playerId);
            player.displayClientMessage(
                Component.translatable("skill.corpseorigin.multi_eye.ended")
                    .withStyle(net.minecraft.ChatFormatting.GRAY),
                true
            );
            return;
        }

        // 执行感知效果
        performPerception(player);
    }

    /**
     * 执行感知效果
     * 检测周围敌意生物并给予高亮效果
     */
    private static void performPerception(Player player) {
        Level level = player.level();
        Vec3 playerPos = player.position();

        // 创建检测范围
        AABB detectionBox = new AABB(
            playerPos.x - DETECTION_RANGE,
            playerPos.y - DETECTION_RANGE / 2,
            playerPos.z - DETECTION_RANGE,
            playerPos.x + DETECTION_RANGE,
            playerPos.y + DETECTION_RANGE / 2,
            playerPos.z + DETECTION_RANGE
        );

        // 获取范围内所有生物
        List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class,
            detectionBox,
            entity -> entity != player && isHostile(entity, player)
        );

        // 对敌意生物施加效果
        for (LivingEntity entity : entities) {
            double distance = player.distanceTo(entity);

            // 在高亮范围内的敌人给予发光效果
            if (distance <= HIGHLIGHT_RANGE) {
                entity.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    20, // 1秒持续时间，持续刷新
                    0,
                    false,
                    false,
                    false
                ));

                // 给予玩家伤害提升（对感知到的敌人）
                if (!player.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST,
                        40,
                        0,
                        false,
                        false,
                        false
                    ));
                }
            }

            // 生成粒子效果指向敌人
            if (level instanceof ServerLevel serverLevel && distance <= HIGHLIGHT_RANGE) {
                spawnDirectionalParticles(serverLevel, player, entity);
            }
        }

        // 显示感知到的敌人数量
        int hostileCount = entities.size();
        if (hostileCount > 0 && level.getGameTime() % 20 == 0) { // 每秒更新一次
            player.displayClientMessage(
                Component.translatable("skill.corpseorigin.multi_eye.detected", hostileCount)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW),
                true
            );
        }
    }

    /**
     * 判断实体是否对玩家有敌意
     */
    private static boolean isHostile(LivingEntity entity, Player player) {
        // 检查是否是怪物
        if (entity instanceof Mob mob) {
            // 检查怪物是否以玩家为目标
            if (mob.getTarget() == player) {
                return true;
            }

            // 检查怪物是否在攻击玩家
            LivingEntity lastHurtByMob = mob.getLastHurtByMob();
            if (lastHurtByMob == player) {
                return true;
            }

            // 检查玩家是否是怪物的敌人
            if (mob.getLastHurtByMobTimestamp() > 0 &&
                mob.getLastHurtByMob() instanceof Player) {
                return true;
            }

            // 检查怪物类型（敌对生物）
            return mob.getClassification(false).equals(net.minecraft.world.entity.MobCategory.MONSTER);
        }

        // 检查其他玩家（PVP情况）
        if (entity instanceof Player otherPlayer) {
            // 如果其他玩家最近攻击过该玩家
            if (player.getLastHurtByMob() == otherPlayer &&
                player.tickCount - player.getLastHurtByMobTimestamp() < 200) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成指向敌人的粒子效果
     */
    private static void spawnDirectionalParticles(ServerLevel level, Player player, LivingEntity target) {
        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 direction = targetPos.subtract(playerPos).normalize();

        // 在玩家周围生成粒子指向敌人
        for (int i = 1; i <= 5; i++) {
            Vec3 particlePos = playerPos.add(direction.scale(i * 0.5));
            level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                1,
                0.05,
                0.05,
                0.05,
                0.01
            );
        }
    }

    /**
     * 检查技能是否处于激活状态
     */
    public static boolean isActive(Player player) {
        if (player.level().isClientSide) {
            return false;
        }

        UUID playerId = player.getUUID();
        if (!activeMap.containsKey(playerId)) {
            return false;
        }

        long currentTime = player.level().getGameTime();
        long activationTime = activeMap.get(playerId);
        return currentTime - activationTime <= DURATION_TICKS;
    }

    /**
     * 获取剩余冷却时间（tick）
     */
    public int getCooldownRemaining(Player player) {
        if (player.level().isClientSide) {
            return 0;
        }

        UUID playerId = player.getUUID();
        if (!cooldownMap.containsKey(playerId)) {
            return 0;
        }

        long currentTime = player.level().getGameTime();
        long lastUseTime = cooldownMap.get(playerId);
        long remainingTicks = COOLDOWN_TICKS - (currentTime - lastUseTime);

        return (int) Math.max(0, remainingTicks);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, path);
    }
}
