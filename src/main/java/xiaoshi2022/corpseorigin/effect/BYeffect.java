package xiaoshi2022.corpseorigin.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BYeffect extends MobEffect {

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

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity livingEntity, int amplifier) {
        performTransformation(livingEntity, level);
        return true;
    }

    /**
     * 执行转化逻辑 - ✅ 添加玩家转化
     */
    private void performTransformation(LivingEntity livingEntity, ServerLevel serverLevel) {
        // 村民转化为尸兄
        if (livingEntity instanceof Villager villager) {
            convertVillagerToZb(villager, serverLevel);
        }
        // ✅ 玩家转化为尸族
        else if (livingEntity instanceof ServerPlayer player) {
            convertPlayerToCorpse(player);
        }

        infectionSource.remove(livingEntity.getUUID());
    }

    /**
     * 将村民转化为尸兄
     */
    private void convertVillagerToZb(Villager villager, ServerLevel serverLevel) {
        try {
            LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, serverLevel);
            zb.setPos(villager.getX(), villager.getY(), villager.getZ());
            zb.setYRot(villager.getYRot());
            zb.setXRot(villager.getXRot());

            String villagerName = villager.getName().getString();
            zb.setPlayerSkinName(villagerName);
            zb.setCustomName(villager.getCustomName());
            zb.setCustomNameVisible(villager.isCustomNameVisible());

            UUID sourceUUID = infectionSource.get(villager.getUUID());
            if (sourceUUID != null) {
                CorpseOrigin.LOGGER.info("村民 {} 转化为尸兄，感染源: {}", villagerName, sourceUUID);
            }

            villager.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            serverLevel.addFreshEntity(zb);

            CorpseOrigin.LOGGER.info("村民 {} 成功变异为尸兄", villagerName);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("村民转化失败: {}", e.getMessage());
        }
    }

    /**
     * ✅ 将玩家转化为尸族
     */
    private void convertPlayerToCorpse(ServerPlayer player) {
        // 设置玩家为尸族状态
        PlayerCorpseComponent.setPlayerAsCorpse(player, 1);

        boolean hasConsciousness = PlayerCorpseComponent.get(player).hasInnateConsciousness();

        // 广播给所有玩家
        CorpseNetwork.broadcastPlayerCorpseSync(player);

        // 播放转化特效
        player.level().broadcastEntityEvent(player, (byte) 35);

        // 消息
        if (hasConsciousness) {
            player.sendSystemMessage(Component.literal(
                    "§c§l你已被感染成为尸兄！§r\n" +
                            "§a§l幸运的是，你保留了人类的意识！§r\n" +
                            "§7击杀生物可获得进化点来解锁更多技能！"
            ));
            CorpseOrigin.LOGGER.info("玩家 {} 已转化为尸族！幸运地保留了意识！", player.getName().getString());
        } else {
            player.sendSystemMessage(Component.literal(
                    "§c§l你已被感染成为尸兄！§r\n" +
                            "§4§l你的意识被黑暗吞噬，只剩下本能...§r\n" +
                            "§7寻找穆博士的眼睛 或 进化到3级 可恢复意识"
            ));
            CorpseOrigin.LOGGER.info("玩家 {} 已转化为尸族！失去了人类意识...", player.getName().getString());
        }
    }

    // ==================== 静态方法 ====================

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel) {
        applyInfection(target, serverLevel, null);
    }

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;

        if (target.hasEffect(ModEffects.QIANS)) {
            return;
        }

        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

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

    public static boolean canInfect(LivingEntity target) {
        if (target instanceof Villager) {
            return true;
        }
        // ✅ 玩家可以被感染（非尸族玩家）
        if (target instanceof ServerPlayer player) {
            return !PlayerCorpseComponent.isCorpse(player);
        }
        return false;
    }

    public static UUID getInfectionSource(UUID targetUUID) {
        return infectionSource.get(targetUUID);
    }

    public static void clearInfectionSource(UUID targetUUID) {
        infectionSource.remove(targetUUID);
    }
}