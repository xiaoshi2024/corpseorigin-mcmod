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

    /** 感染源记录 */
    private static final Map<UUID, UUID> infectionSource = new HashMap<>();

    /** ✅ 记录每个目标的初始感染总时长（tick） */
    private static final Map<UUID, Integer> TOTAL_DURATION = new HashMap<>();

    public BYeffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        super.onEffectAdded(livingEntity, amplifier);
        if (!livingEntity.level().isClientSide()) {
            // ✅ 如果没有记录，用当前效果时长补一个（兜底）
            if (!TOTAL_DURATION.containsKey(livingEntity.getUUID())) {
                MobEffectInstance instance = livingEntity.getEffect(ModEffects.QIANS);
                if (instance != null) {
                    TOTAL_DURATION.put(livingEntity.getUUID(), instance.getDuration());
                }
            }
            CorpseOrigin.LOGGER.info("感染效果添加到 {}，将在效果结束时变异",
                    livingEntity.getName().getString());
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // ✅ 每 10 tick 更新一次感染度；最后一 tick 必须触发转化
        return duration % 10 == 0 || duration <= 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity livingEntity, int amplifier) {
        MobEffectInstance instance = livingEntity.getEffect(ModEffects.QIANS);
        if (instance == null) {
            return true;
        }

        int remaining = instance.getDuration();

        // ===== 最后一 tick：转化 =====
        if (remaining <= 1) {
            performTransformation(livingEntity, level);

            // 清理 + 清零感染度
            TOTAL_DURATION.remove(livingEntity.getUUID());
            infectionSource.remove(livingEntity.getUUID());

            if (livingEntity instanceof ServerPlayer player) {
                PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
                comp.setInfection(0);
                CorpseNetwork.sendInfectionSync(player);
            }
            return true;
        }

        // ===== 中间：按比例更新感染度（只对玩家）=====
        if (livingEntity instanceof ServerPlayer player) {
            Integer total = TOTAL_DURATION.get(player.getUUID());
            if (total == null || total <= 0) {
                // 兜底：没有记录就跳过
                return true;
            }

            int infection = (int) ((total - remaining) * 100.0 / total);
            infection = Math.max(0, Math.min(100, infection));

            PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
            comp.setInfection(infection);
            CorpseNetwork.sendInfectionSync(player);
        }

        return true;
    }

    /**
     * 执行转化逻辑
     */
    private void performTransformation(LivingEntity livingEntity, ServerLevel serverLevel) {
        if (livingEntity instanceof Villager villager) {
            convertVillagerToZb(villager, serverLevel);
        } else if (livingEntity instanceof ServerPlayer player) {
            convertPlayerToCorpse(player);
        }
    }

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

    private void convertPlayerToCorpse(ServerPlayer player) {
        PlayerCorpseComponent.setPlayerAsCorpse(player, 1);

        boolean hasConsciousness = PlayerCorpseComponent.get(player).hasInnateConsciousness();

        CorpseNetwork.broadcastPlayerCorpseSync(player);
        player.level().broadcastEntityEvent(player, (byte) 35);

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
        if (target.hasEffect(ModEffects.QIANS)) return;

        int duration = 60 + serverLevel.getRandom().nextInt(241);

        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // ✅ 记录总时长（必须在 addEffect 之前或之后都行，只要在 effect tick 之前）
        TOTAL_DURATION.put(target.getUUID(), duration);

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

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel,
                                      int durationTicks, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;
        if (target.hasEffect(ModEffects.QIANS)) return;

        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // ✅ 记录总时长
        TOTAL_DURATION.put(target.getUUID(), durationTicks);

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

    /** ✅ 外部清理（玩家退出/effect 被强制移除时调用） */
    public static void clearTotalDuration(UUID targetUUID) {
        TOTAL_DURATION.remove(targetUUID);
    }
}