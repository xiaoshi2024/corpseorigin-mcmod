/**
 * 尸兄感染效果类 - 核心转化机制
 *
 * 【功能说明】
 * 1. 村民转化：感染效果结束时，村民转化为尸兄实体
 * 2. 玩家转化：感染效果结束时，玩家转化为尸族并获得技能系统
 * 3. 感染源追踪：记录是谁传播的感染，用于建立主从关系
 *
 * 【转化流程】
 * 1. 应用感染效果（随机延迟3-15秒或自定义时间）
 * 2. 效果持续期间显示感染图标
 * 3. 效果结束时执行转化逻辑
 * 4. 村民 -> 尸兄实体（会继承原村民名字）
 * 5. 玩家 -> 尸族状态 + 初始技能 + 进化点
 *
 * 【感染源系统】
 * - 记录感染者UUID -> 感染源UUID的映射
 * - 用于建立尸王与手下尸兄的主从关系
 * - 尸王可以感知到自己感染转化的尸兄
 *
 * 【关联系统】
 * - LowerLevelZbEntity: 转化后的尸兄实体
 * - PlayerCorpseData: 玩家尸族状态管理
 * - CorpseKingData: 尸王数据存储
 * - SkillAttachment: 玩家技能系统
 *
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.effect;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.data.CorpseKingData;
import com.phagens.corpseorigin.network.PlayerCorpseSyncPacket;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.register.EffectRegister;
import com.phagens.corpseorigin.register.EntityRegistry;
import com.phagens.corpseorigin.skill.CorpseSkills;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 尸兄感染效果
 * 继承MobEffect实现自定义的感染转化逻辑
 */
public class BYeffect extends MobEffect {

    /** 存储感染源映射：被感染者UUID -> 感染者（尸王）UUID */
    private static final Map<UUID, UUID> infectionSource = new HashMap<>();

    /**
     * 构造函数
     *
     * @param category 效果类别（通常为HARMFUL）
     * @param color 效果颜色（用于粒子效果）
     */
    public BYeffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * 效果被添加到实体时的处理
     * 记录日志用于调试
     */
    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        super.onEffectAdded(livingEntity, amplifier);
        if (!livingEntity.level().isClientSide) {
            CorpseOrigin.LOGGER.info("感染效果添加到 {}，将在效果结束时变异",
                    livingEntity.getName().getString());
        }
    }

    /**
     * 判断是否应该在本tick应用效果
     * 只在效果的最后1 tick返回true，确保转化只执行一次
     *
     * @param duration 剩余持续时间
     * @param amplifier 效果等级
     * @return 是否应用效果
     */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1;
    }

    /**
     * 应用效果tick
     * 在效果结束时执行转化逻辑
     *
     * @param livingEntity 目标实体
     * @param amplifier 效果等级
     * @return 是否成功应用
     */
    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.level().isClientSide && livingEntity.level() instanceof ServerLevel serverLevel) {
            performTransformation(livingEntity, serverLevel);
        }
        return true;
    }

    /**
     * 执行转化逻辑
     * 根据实体类型决定转化方式
     *
     * @param livingEntity 要被转化的实体
     * @param serverLevel 服务器世界
     */
    private void performTransformation(LivingEntity livingEntity, ServerLevel serverLevel) {
        // 村民转化为尸兄
        if (livingEntity instanceof Villager villager) {
            convertVillagerToZb(villager, serverLevel);
        }
        // 玩家转化为尸族
        else if (livingEntity instanceof ServerPlayer player) {
            convertPlayerToCorpse(player);
        }

        // 转化完成后清除感染源记录
        infectionSource.remove(livingEntity.getUUID());
    }

    /**
     * 将村民转化为尸兄实体
     *
     * 【转化过程】
     * 1. 创建新的尸兄实体
     * 2. 复制位置和旋转信息
     * 3. 随机设置变种（30%概率为裂口尸兄）
     * 4. 设置皮肤名称（继承村民名字）
     * 5. 检查感染源，建立主从关系
     * 6. 移除原村民，添加尸兄到世界
     *
     * @param villager 要被转化的村民
     * @param serverLevel 服务器世界
     */
    private void convertVillagerToZb(Villager villager, ServerLevel serverLevel) {
        // 检查实体是否已注册
        if (EntityRegistry.LOWER_LEVEL_ZB.get() == null) {
            CorpseOrigin.LOGGER.error("LOWER_LEVEL_ZB 实体未注册！");
            return;
        }

        try {
            // 创建尸兄实体
            LowerLevelZbEntity zb = new LowerLevelZbEntity(EntityRegistry.LOWER_LEVEL_ZB.get(), serverLevel);
            zb.setPos(villager.getX(), villager.getY(), villager.getZ());
            zb.setYRot(villager.getYRot());
            zb.setXRot(villager.getXRot());

            // 随机设置变种类型，30%概率为裂口尸兄
            if (serverLevel.getRandom().nextFloat() < 0.3) {
                zb.setVariant(LowerLevelZbEntity.Variant.CRACKED);
            }

            // 设置皮肤为村民的名字（如果有）
            String villagerName = villager.getName().getString();
            zb.setPlayerSkinName(villagerName);

            // 复制村民的一些属性
            zb.setCustomName(villager.getCustomName());
            zb.setCustomNameVisible(villager.isCustomNameVisible());

            // 检查是否有感染源（尸王），如果有则设置为手下
            UUID sourceUUID = infectionSource.get(villager.getUUID());
            if (sourceUUID != null) {
                zb.setMaster(sourceUUID);
                // 添加到尸王的手下列表
                CorpseKingData data = CorpseKingData.get(serverLevel);
                data.addMinion(sourceUUID, zb.getUUID());

                // 通知尸王
                Player master = serverLevel.getServer().getPlayerList().getPlayer(sourceUUID);
                if (master != null) {
                    master.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a§l你感染的村民已转化为尸兄手下！"
                    ));
                }

                CorpseOrigin.LOGGER.info("村民 {} 转化为尸兄，已成为尸王 {} 的手下",
                        villagerName, sourceUUID);
            }

            // 移除村民
            villager.remove(Entity.RemovalReason.CHANGED_DIMENSION);

            // 添加尸兄到世界
            serverLevel.addFreshEntity(zb);

            // 播放转化特效
            serverLevel.broadcastEntityEvent(zb, (byte) 35);

            CorpseOrigin.LOGGER.info("村民 {} 成功变异为尸兄", villagerName);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("村民转化失败: {}", e.getMessage());
        }
    }

    /**
     * 静态方法：给实体添加感染效果（带随机延迟3-15秒）
     *
     * @param target 目标实体
     * @param serverLevel 服务器世界
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel) {
        applyInfection(target, serverLevel, null);
    }

    /**
     * 静态方法：给实体添加感染效果（带随机延迟3-15秒）
     * 带感染源版本，用于建立主从关系
     *
     * @param target 目标实体
     * @param serverLevel 服务器世界
     * @param sourceUUID 感染源UUID（尸王）
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;

        // 检查目标是否已经被感染，避免重复感染
        if (target.hasEffect(EffectRegister.QIANS)) {
            CorpseOrigin.LOGGER.debug("目标 {} 已经被感染，跳过重复感染",
                    target.getName().getString());
            return;
        }

        // 检查玩家是否已经是尸兄，避免重复感染
        if (target instanceof Player player && PlayerCorpseData.isCorpse(player)) {
            CorpseOrigin.LOGGER.debug("玩家 {} 已经是尸兄，跳过感染",
                    player.getName().getString());
            return;
        }

        // 记录感染源
        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // 随机延迟3-15秒（60-300 ticks）
        int duration = 60 + serverLevel.getRandom().nextInt(241);

        // 添加感染效果
        target.addEffect(new MobEffectInstance(
                EffectRegister.QIANS,
                duration,
                0,                      // 放大器
                false,                  // 不显示粒子
                true,                   // 显示图标
                true
        ));

        CorpseOrigin.LOGGER.info("感染效果已应用到 {}，将在 {} 秒后变异",
                target.getName().getString(), duration / 20);
    }

    /**
     * 静态方法：给实体添加感染效果（自定义延迟）
     *
     * @param target 目标实体
     * @param serverLevel 服务器世界
     * @param durationTicks 持续时间（tick）
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, int durationTicks) {
        applyInfection(target, serverLevel, durationTicks, null);
    }

    /**
     * 静态方法：给实体添加感染效果（自定义延迟）
     * 带感染源版本
     *
     * @param target 目标实体
     * @param serverLevel 服务器世界
     * @param durationTicks 持续时间（tick）
     * @param sourceUUID 感染源UUID（尸王）
     */
    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, int durationTicks, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;

        // 检查目标是否已经被感染，避免重复感染
        if (target.hasEffect(EffectRegister.QIANS)) {
            CorpseOrigin.LOGGER.debug("目标 {} 已经被感染，跳过重复感染",
                    target.getName().getString());
            return;
        }

        // 检查玩家是否已经是尸兄，避免重复感染
        if (target instanceof Player player && PlayerCorpseData.isCorpse(player)) {
            CorpseOrigin.LOGGER.debug("玩家 {} 已经是尸兄，跳过感染",
                    player.getName().getString());
            return;
        }

        // 记录感染源
        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // 添加感染效果
        target.addEffect(new MobEffectInstance(
                EffectRegister.QIANS,
                durationTicks,
                0,
                false,
                true,
                true
        ));

        CorpseOrigin.LOGGER.info("感染效果已应用到 {}，将在 {} 秒后变异",
                target.getName().getString(), durationTicks / 20);
    }

    /**
     * 将玩家转化为尸族
     *
     * 【转化过程】
     * 1. 设置玩家为尸族状态
     * 2. 进行意识判定（5%概率保留意识）
     * 3. 初始化技能系统
     * 4. 自动解锁初始技能（硬化皮肤）
     * 5. 给予5点初始进化点
     * 6. 同步数据到客户端
     * 7. 播放转化特效
     * 8. 发送提示消息（根据意识状态）
     *
     * @param player 要被转化的玩家
     */
    private void convertPlayerToCorpse(ServerPlayer player) {
        // 设置玩家为尸族状态
        PlayerCorpseData.setPlayerAsCorpse(player, 1);

        // 检查是否保留意识
        boolean hasConsciousness = PlayerCorpseData.hasInnateConsciousness(player);

        // 初始化技能系统
        ISkillHandler skillHandler = SkillAttachment.getSkillHandler(player);

        // 自动解锁初始技能（硬化皮肤）
        if (!skillHandler.hasLearned(CorpseSkills.HARDENED_SKIN.getId())) {
            skillHandler.learnSkill(CorpseSkills.HARDENED_SKIN);
            CorpseOrigin.LOGGER.info("玩家 {} 解锁初始技能：硬化皮肤", player.getName().getString());
        }

        // 给予初始进化点
        skillHandler.addEvolutionPoints(5);

        // 同步技能数据到客户端
        skillHandler.syncToClient();

        // 同步尸族状态到客户端
        PlayerCorpseSyncPacket packet = new PlayerCorpseSyncPacket(
                player.getId(), true, 1, PlayerCorpseData.getCorpseData(player)
        );
        // 发送给所有在线玩家，确保所有人都能看到尸兄状态
        PacketDistributor.sendToAllPlayers(packet);

        // 播放转化特效
        player.level().broadcastEntityEvent(player, (byte) 35);

        // 检查感染源是否有感染玩家的任务，更新任务进度
        UUID sourceUUID = infectionSource.get(player.getUUID());
        if (sourceUUID != null) {
            ServerPlayer infector = player.server.getPlayerList().getPlayer(sourceUUID);
            if (infector != null) {
                updateInfectPlayerMission(infector);
            }
        }

        // 根据意识状态发送不同的提示消息
        if (hasConsciousness) {
            // 保留意识的幸运儿
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c§l你已被感染成为尸兄！§r\n" +
                    "§a§l幸运的是，你保留了人类的意识！§r\n" +
                    "§7按 §eK§7 打开技能树，按 §eR§7 打开技能轮盘\n" +
                    "§7击杀生物可获得进化点来解锁更多技能！"
            ));
            CorpseOrigin.LOGGER.info("玩家 {} 已转化为尸族！幸运地保留了意识！", player.getName().getString());
        } else {
            // 失去意识的普通尸兄
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c§l你已被感染成为尸兄！§r\n" +
                    "§4§l你的意识被黑暗吞噬，只剩下本能...§r\n" +
                    "§7你无法使用工作台、门等复杂物品\n" +
                    "§7§o寻找穆博士的眼睛 或 进化到3级 可恢复意识"
            ));
            CorpseOrigin.LOGGER.info("玩家 {} 已转化为尸族！失去了人类意识...", player.getName().getString());
        }
    }

    /**
     * 更新感染玩家任务进度
     */
    private void updateInfectPlayerMission(ServerPlayer infector) {
        for (int i = 0; i < infector.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = infector.getInventory().getItem(i);
            if (stack.getItem() instanceof com.phagens.corpseorigin.Item.MissionScrollItem) {
                String missionType = com.phagens.corpseorigin.Item.MissionScrollItem.getMissionType(stack);
                
                if ("infect_player".equals(missionType) && !com.phagens.corpseorigin.Item.MissionScrollItem.isCompleted(stack)) {
                    com.phagens.corpseorigin.Item.MissionScrollItem.incrementCount(stack);
                    
                    if (com.phagens.corpseorigin.Item.MissionScrollItem.isCompleted(stack)) {
                        infector.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.mission_completed"));
                    } else {
                        int remaining = com.phagens.corpseorigin.Item.MissionScrollItem.getTargetCount(stack) - 
                                       com.phagens.corpseorigin.Item.MissionScrollItem.getCurrentCount(stack);
                        infector.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.mission_progress", remaining));
                    }
                }
            }
        }
    }

    /**
     * 检查目标是否可以被感染
     *
     * @param target 目标实体
     * @return 是否可以被感染
     */
    public static boolean canInfect(LivingEntity target) {
        // 村民可以被感染
        if (target instanceof Villager) {
            return true;
        }

        // 玩家可以被感染，但已经是尸兄的玩家除外
        if (target instanceof Player player) {
            return !PlayerCorpseData.isCorpse(player);
        }

        return false;
    }

    /**
     * 获取感染源
     *
     * @param targetUUID 被感染者UUID
     * @return 感染源UUID（尸王）
     */
    public static UUID getInfectionSource(UUID targetUUID) {
        return infectionSource.get(targetUUID);
    }

    /**
     * 清除感染源记录
     *
     * @param targetUUID 被感染者UUID
     */
    public static void clearInfectionSource(UUID targetUUID) {
        infectionSource.remove(targetUUID);
    }
}
