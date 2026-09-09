package xiaoshi2022.corpseorigin.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 尸兄感染效果 - 核心转化机制
 *
 * 【功能说明】
 * 1. 村民转化：感染效果结束时，村民转化为尸兄实体
 * 2. 感染源追踪：记录是谁传播的感染
 */
public class BYeffect extends MobEffect {

    /** 存储感染源映射：被感染者UUID -> 感染者UUID */
    private static final Map<UUID, UUID> infectionSource = new HashMap<>();

    public BYeffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        super.onEffectAdded(livingEntity, amplifier);
        if (!livingEntity.level().isClientSide()) {
            CorpseOrigin.LOGGER.info("感染效果添加到 {}，将在效果结束时变异",
                    livingEntity.getName().getString());
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1;
    }

    // ✅ 修复：Fabric 26.2 中 applyEffectTick 签名变了
    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity livingEntity, int amplifier) {
        performTransformation(livingEntity, level);
        return true;
    }

    /**
     * 执行转化逻辑
     */
    private void performTransformation(LivingEntity livingEntity, ServerLevel serverLevel) {
        // 村民转化为尸兄
        if (livingEntity instanceof Villager villager) {
            convertVillagerToZb(villager, serverLevel);
        }

        // 清除感染源记录
        infectionSource.remove(livingEntity.getUUID());
    }

    /**
     * 将村民转化为尸兄实体
     */
    private void convertVillagerToZb(Villager villager, ServerLevel serverLevel) {
        try {
            // 创建尸兄实体
            LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, serverLevel);
            zb.setPos(villager.getX(), villager.getY(), villager.getZ());
            zb.setYRot(villager.getYRot());
            zb.setXRot(villager.getXRot());

            // 设置皮肤为村民的名字
            String villagerName = villager.getName().getString();
            zb.setPlayerSkinName(villagerName);
            zb.setCustomName(villager.getCustomName());
            zb.setCustomNameVisible(villager.isCustomNameVisible());

            // 检查是否有感染源
            UUID sourceUUID = infectionSource.get(villager.getUUID());
            if (sourceUUID != null) {
                CorpseOrigin.LOGGER.info("村民 {} 转化为尸兄，感染源: {}", villagerName, sourceUUID);
                // TODO: 后续可扩展尸王系统
            }

            // 移除村民，添加尸兄
            villager.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            serverLevel.addFreshEntity(zb);

            CorpseOrigin.LOGGER.info("村民 {} 成功变异为尸兄", villagerName);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("村民转化失败: {}", e.getMessage());
        }
    }

    // ==================== 静态方法：应用感染 ====================

    /**
     * 给实体添加感染效果（随机延迟3-15秒）
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel) {
        applyInfection(target, serverLevel, null);
    }

    /**
     * 给实体添加感染效果（随机延迟3-15秒，带感染源）
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;

        // ✅ 使用 ModEffects.QIANS（需要导入）
        if (target.hasEffect(ModEffects.QIANS)) {
            return;
        }

        // 记录感染源
        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // 随机延迟3-15秒（60-300 ticks）
        int duration = 60 + serverLevel.getRandom().nextInt(241);

        target.addEffect(new MobEffectInstance(
                ModEffects.QIANS,
                duration,
                0,
                false,
                true,
                true
        ));

        CorpseOrigin.LOGGER.info("感染效果已应用到 {}，将在 {} 秒后变异",
                target.getName().getString(), duration / 20);
    }

    /**
     * 给实体添加感染效果（自定义延迟）
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, int durationTicks, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;

        if (target.hasEffect(ModEffects.QIANS)) {
            return;
        }

        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        target.addEffect(new MobEffectInstance(
                ModEffects.QIANS,
                durationTicks,
                0,
                false,
                true,
                true
        ));
    }

    /**
     * 检查目标是否可以被感染
     */
    public static boolean canInfect(LivingEntity target) {
        return target instanceof Villager;
    }

    /**
     * 获取感染源
     */
    public static UUID getInfectionSource(UUID targetUUID) {
        return infectionSource.get(targetUUID);
    }

    /**
     * 清除感染源记录
     */
    public static void clearInfectionSource(UUID targetUUID) {
        infectionSource.remove(targetUUID);
    }
}