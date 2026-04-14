package com.phagens.corpseorigin.skill.special;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.data.CorpseKingData;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import com.phagens.corpseorigin.skill.BaseSkill;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 尸王之力技能 - 模拟龙右的感染和控制能力
 *
 * 【功能说明】
 * 1. 主动技能：激活后5秒内攻击目标可触发特殊效果
 * 2. 控制玩家：攻击玩家可控制3秒（可强制定身）
 * 3. 收服尸兄：攻击尸兄NPC可永久收为手下
 * 4. 感染转化：攻击村民可转化为尸兄手下
 *
 * 【技能参数】
 * - CONTROL_RANGE: 16格控制范围
 * - PLAYER_CONTROL_DURATION: 60 tick = 3秒控制时间
 * - SKILL_COOLDOWN: 300 tick = 15秒冷却时间
 * - SKILL_ACTIVE_DURATION: 100 tick = 5秒激活持续时间
 *
 * 【控制效果】
 * - 被控制玩家获得发光、虚弱、缓慢效果
 * - 被控制玩家强制看向控制者方向
 * - 可切换定身状态（完全锁定移动）
 * - 控制结束后自动清除所有效果
 *
 * 【数据管理】
 * - activeSkillPlayers: 记录技能处于激活状态的玩家
 * - controlledPlayers: 记录被控制的玩家信息
 * - zombieMinions: 记录尸王的手下集合
 * - CorpseKingData: 持久化存储控制关系
 *
 * 【关联系统】
 * - CorpseKingControlHandler: 处理被控制玩家的tick逻辑
 * - CorpseKingData: 数据持久化
 * - BYeffect: 村民感染转化
 * - LowerLevelZbEntity: 尸兄手下实体
 *
 * @author Phagens
 * @version 1.0
 */
public class CorpseKingPowerSkill extends BaseSkill {

    /** 控制范围（格） */
    private static final double CONTROL_RANGE = 16.0D;
    /** 玩家控制持续时间（tick）- 3秒 */
    private static final int PLAYER_CONTROL_DURATION = 60;
    /** 技能冷却时间（tick）- 15秒 */
    private static final int SKILL_COOLDOWN = 300;
    /** 技能激活持续时间（tick）- 5秒 */
    private static final int SKILL_ACTIVE_DURATION = 100;

    /** 存储技能激活状态的玩家：玩家UUID -> 激活结束时间 */
    private static final Map<UUID, Long> activeSkillPlayers = new HashMap<>();
    /** 存储被控制的玩家信息：被控制者UUID -> 控制信息 */
    private static final Map<UUID, PlayerControlInfo> controlledPlayers = new HashMap<>();
    /** 存储尸王的手下：尸王UUID -> 手下UUID集合 */
    private static final Map<UUID, Set<UUID>> zombieMinions = new HashMap<>();

    /**
     * 构造函数
     *
     * @param builder 技能构建器
     */
    public CorpseKingPowerSkill(Builder builder) {
        super(builder);
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel level = serverPlayer.serverLevel();

        CorpseOrigin.LOGGER.info("玩家 {} 激活尸王之力", player.getName().getString());

        // 记录技能激活状态
        long endTime = System.currentTimeMillis() + (SKILL_ACTIVE_DURATION * 50L);
        activeSkillPlayers.put(player.getUUID(), endTime);

        // 激活时给予玩家一个临时的攻击增强效果
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, SKILL_ACTIVE_DURATION, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SKILL_ACTIVE_DURATION, 0));
        // 添加粒子效果表示技能已激活
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, SKILL_ACTIVE_DURATION, 0, false, false));

        player.sendSystemMessage(Component.literal("§4§l尸王之力已激活！5秒内攻击目标可控制玩家或收服尸兄！"));

        // 设置技能冷却
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        if (handler != null) {
            handler.setCooldown(this, SKILL_COOLDOWN);
        }

        // 播放激活音效
        level.playSound(null, player.blockPosition(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /**
     * 检查玩家是否处于尸王之力激活状态
     */
    public static boolean isSkillActive(Player player) {
        Long endTime = activeSkillPlayers.get(player.getUUID());
        if (endTime == null) return false;
        if (System.currentTimeMillis() > endTime) {
            activeSkillPlayers.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /**
     * 当尸王攻击目标时调用此方法
     * 只有技能处于激活状态时才会触发效果
     */
    public static void onAttackTarget(Player attacker, net.minecraft.world.entity.LivingEntity target) {
        if (attacker.level().isClientSide) return;
        if (!PlayerCorpseData.isCorpse(attacker)) return;

        // 检查攻击者是否拥有尸王之力技能
        ISkillHandler handler = SkillAttachment.getSkillHandler(attacker);
        if (handler == null || !handler.hasLearned(getSkillId())) return;

        // 检查技能是否处于激活状态
        if (!isSkillActive(attacker)) {
            return; // 技能未激活，不触发效果
        }

        ServerLevel level = (ServerLevel) attacker.level();

        // 根据目标类型执行不同逻辑
        if (target instanceof Player targetPlayer) {
            // 目标是玩家 - 控制3秒
            controlPlayer(attacker, targetPlayer, level);
        } else if (target instanceof LowerLevelZbEntity zbEntity) {
            // 目标是尸兄NPC - 永久收为手下
            recruitMinion(attacker, zbEntity, level);
        } else if (target instanceof net.minecraft.world.entity.npc.Villager villager) {
            // 目标是村民 - 转化为尸兄并收为手下
            convertVillagerToMinion(attacker, villager, level);
        }
    }

    /**
     * 控制玩家3秒
     */
    private static void controlPlayer(Player controller, Player target, ServerLevel level) {
        // 不能控制已成为尸兄的玩家
        if (PlayerCorpseData.isCorpse(target)) return;
        // 不能控制创造/旁观模式玩家
        if (target.isCreative() || target.isSpectator()) return;

        CorpseOrigin.LOGGER.info("尸王 {} 控制玩家 {} 3秒", controller.getName().getString(), target.getName().getString());

        // 创建控制信息
        PlayerControlInfo controlInfo = new PlayerControlInfo(
                controller.getUUID(),
                System.currentTimeMillis(),
                PLAYER_CONTROL_DURATION,
                true // 默认定身
        );
        controlledPlayers.put(target.getUUID(), controlInfo);

        // 给被控制玩家添加效果
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, PLAYER_CONTROL_DURATION, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PLAYER_CONTROL_DURATION, 1, false, false));
        // 定身效果（通过缓慢实现禁止移动）
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PLAYER_CONTROL_DURATION, 10, false, false));

        // 播放效果
        level.sendParticles(
                ParticleTypes.SOUL,
                target.getX(), target.getY() + 1, target.getZ(),
                30, 0.5, 1, 0.5, 0.05
        );
        level.playSound(null, target.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.5F);

        // 发送消息
        controller.sendSystemMessage(Component.literal("§4§l你控制了 " + target.getName().getString() + "！持续3秒！"));
        target.sendSystemMessage(Component.literal("§c§l你被尸王控制了！无法动弹！"));

        // 保存到数据
        CorpseKingData data = CorpseKingData.get(level);
        data.addPlayerControl(target.getUUID(), controller.getUUID(), PLAYER_CONTROL_DURATION);
    }

    /**
     * 收服尸兄为手下（永久）
     */
    private static void recruitMinion(Player master, LowerLevelZbEntity minion, ServerLevel level) {
        // 检查是否已经是主人的手下
        if (minion.getMasterUUID() != null && minion.getMasterUUID().equals(master.getUUID())) {
            master.sendSystemMessage(Component.literal("§7这个尸兄已经是你的手下了！"));
            return;
        }

        // 检查是否已经是其他尸王的手下
        if (isMinionOfOther(master, minion)) {
            master.sendSystemMessage(Component.literal("§c这个尸兄已经是其他尸王的手下了！"));
            return;
        }

        CorpseOrigin.LOGGER.info("尸王 {} 收服尸兄 {} 为手下", master.getName().getString(), minion.getId());

        // 添加到手下列表
        zombieMinions.computeIfAbsent(master.getUUID(), k -> new HashSet<>()).add(minion.getUUID());

        // 设置尸兄的主人
        minion.setMaster(master.getUUID());

        // 播放效果
        level.sendParticles(
                ParticleTypes.HEART,
                minion.getX(), minion.getY() + 1, minion.getZ(),
                10, 0.5, 0.5, 0.5, 0.1
        );
        level.playSound(null, minion.blockPosition(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.0F, 1.2F);

        // 发送消息
        master.sendSystemMessage(Component.literal("§a§l你收服了尸兄作为手下！它将永远追随你！"));

        // 保存到数据
        CorpseKingData data = CorpseKingData.get(level);
        data.addMinion(master.getUUID(), minion.getUUID());
    }

    /**
     * 将村民转化为尸兄并收为手下
     * 使用BYeffect的感染方法，传递尸王UUID作为感染源
     */
    private static void convertVillagerToMinion(Player master, net.minecraft.world.entity.npc.Villager villager, ServerLevel level) {
        CorpseOrigin.LOGGER.info("尸王 {} 感染村民", master.getName().getString());

        // 使用BYeffect的感染方法，传递尸王UUID作为感染源
        // 这样转化后的尸兄会自动成为尸王的手下
        com.phagens.corpseorigin.effect.BYeffect.applyInfection(
                villager,
                level,
                100, // 5秒
                master.getUUID() // 感染源：尸王
        );

        // 播放感染效果
        level.sendParticles(
                ParticleTypes.WITCH,
                villager.getX(), villager.getY() + 1, villager.getZ(),
                30, 0.5, 1, 0.5, 0.1
        );
        level.playSound(null, villager.blockPosition(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.0F, 0.8F);

        // 发送消息
        master.sendSystemMessage(Component.literal("§c§l你感染了村民！等待转化..."));
    }

    /**
     * 切换被控制玩家的定身状态
     * 控制者可以调用此方法来解除或重新施加定身
     */
    public static void toggleFreeze(Player controller, Player target) {
        PlayerControlInfo info = controlledPlayers.get(target.getUUID());
        if (info == null || !info.controllerUUID.equals(controller.getUUID())) return;
        if (info.isExpired()) {
            controlledPlayers.remove(target.getUUID());
            return;
        }

        info.frozen = !info.frozen;

        if (info.frozen) {
            // 重新定身
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, info.getRemainingTicks(), 10, false, false));
            controller.sendSystemMessage(Component.literal("§c你定住了 " + target.getName().getString()));
            target.sendSystemMessage(Component.literal("§c你被尸王定住了！"));
        } else {
            // 解除定身（但仍有虚弱效果）
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            controller.sendSystemMessage(Component.literal("§a你解除了 " + target.getName().getString() + " 的定身"));
            target.sendSystemMessage(Component.literal("§a定身解除了，但你仍被控制！"));
        }
    }

    /**
     * 强制被控制玩家定身（控制者主动触发）
     */
    public static void forceFreeze(Player controller, Player target) {
        PlayerControlInfo info = controlledPlayers.get(target.getUUID());
        if (info == null || !info.controllerUUID.equals(controller.getUUID())) return;
        if (info.isExpired()) {
            controlledPlayers.remove(target.getUUID());
            return;
        }

        if (!info.frozen) {
            info.frozen = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, info.getRemainingTicks(), 10, false, false));
        }
    }

    /**
     * 检查玩家是否被控制
     */
    public static boolean isPlayerControlled(Player player) {
        PlayerControlInfo info = controlledPlayers.get(player.getUUID());
        if (info == null) return false;
        if (info.isExpired()) {
            controlledPlayers.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /**
     * 获取控制玩家的控制者UUID
     */
    public static UUID getControllerUUID(Player player) {
        PlayerControlInfo info = controlledPlayers.get(player.getUUID());
        if (info == null || info.isExpired()) {
            controlledPlayers.remove(player.getUUID());
            return null;
        }
        return info.controllerUUID;
    }

    /**
     * 检查玩家是否被定身
     */
    public static boolean isPlayerFrozen(Player player) {
        PlayerControlInfo info = controlledPlayers.get(player.getUUID());
        if (info == null || info.isExpired()) {
            controlledPlayers.remove(player.getUUID());
            return false;
        }
        return info.frozen;
    }

    /**
     * 获取尸王的所有手下
     */
    public static Set<UUID> getMinions(UUID masterUUID) {
        return zombieMinions.getOrDefault(masterUUID, Collections.emptySet());
    }

    /**
     * 检查尸兄是否已经是其他尸王的手下
     */
    private static boolean isMinionOfOther(Player master, LowerLevelZbEntity minion) {
        for (Map.Entry<UUID, Set<UUID>> entry : zombieMinions.entrySet()) {
            if (!entry.getKey().equals(master.getUUID()) && entry.getValue().contains(minion.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新控制状态（每tick调用）
     */
    public static void updateControls(ServerLevel level) {
        if (level == null) return;

        // 清理过期的玩家控制
        Iterator<Map.Entry<UUID, PlayerControlInfo>> iterator = controlledPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerControlInfo> entry = iterator.next();
            PlayerControlInfo info = entry.getValue();

            if (info.isExpired()) {
                // 控制结束
                Player controlled = level.getServer().getPlayerList().getPlayer(entry.getKey());
                Player controller = level.getServer().getPlayerList().getPlayer(info.controllerUUID);

                if (controlled != null) {
                    controlled.sendSystemMessage(Component.literal("§a你摆脱了尸王的控制！"));
                    // 清除效果
                    controlled.removeEffect(MobEffects.GLOWING);
                    controlled.removeEffect(MobEffects.WEAKNESS);
                    controlled.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
                if (controller != null) {
                    controller.sendSystemMessage(Component.literal("§7控制效果已结束"));
                }

                iterator.remove();

                // 更新数据存储
                CorpseKingData data = CorpseKingData.get(level);
                data.removePlayerControl(entry.getKey());
            }
        }
    }

    /**
     * 清除玩家的控制状态
     */
    public static void clearControl(Player player) {
        controlledPlayers.remove(player.getUUID());
        if (!player.level().isClientSide && player.level() instanceof ServerLevel level) {
            CorpseKingData data = CorpseKingData.get(level);
            data.removePlayerControl(player.getUUID());
        }
    }

    /**
     * 移除尸王的手下
     */
    public static void removeMinion(UUID masterUUID, UUID minionUUID) {
        Set<UUID> minions = zombieMinions.get(masterUUID);
        if (minions != null) {
            minions.remove(minionUUID);
        }
    }

    /**
     * 获取技能ID
     */
    private static ResourceLocation getSkillId() {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "corpse_king_power");
    }

    /**
     * 玩家控制信息类
     */
    private static class PlayerControlInfo {
        final UUID controllerUUID;
        final long controlStartTime;
        final int durationTicks;
        boolean frozen;

        PlayerControlInfo(UUID controllerUUID, long controlStartTime, int durationTicks, boolean frozen) {
            this.controllerUUID = controllerUUID;
            this.controlStartTime = controlStartTime;
            this.durationTicks = durationTicks;
            this.frozen = frozen;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - controlStartTime > (durationTicks * 50L);
        }

        int getRemainingTicks() {
            long elapsed = (System.currentTimeMillis() - controlStartTime) / 50;
            return Math.max(0, durationTicks - (int) elapsed);
        }
    }
}
