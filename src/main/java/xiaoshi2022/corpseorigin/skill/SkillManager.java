package xiaoshi2022.corpseorigin.skill;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 技能管理器 - 服务端技能激活与冷却管理（运行时，非持久化）
 */
public final class SkillManager {

    private SkillManager() {
    }

    /** 每玩家 → 技能路径 → 冷却结束时间戳（毫秒） */
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    /**
     * 激活技能（服务端，含校验）。
     *
     * @return true 表示成功激活并发送冷却同步包
     */
    public static boolean activate(ServerPlayer player, String skillPath) {
        if (skillPath == null || skillPath.isEmpty()) {
            return false;
        }

        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        ISkill skill = null;
        for (ISkill s : character.getSkills()) {
            if (s.getId().getPath().equals(skillPath)) {
                skill = s;
                break;
            }
        }
        if (skill == null) {
            CorpseOrigin.LOGGER.warn("玩家 {} 尝试激活不存在的技能: {}",
                    player.getName().getString(), skillPath);
            return false;
        }
        if (!skill.isActivatable()) {
            return false;
        }

        // 校验已学习
        PlayerCharacterData data = PlayerCharacterData.get(player);
        if (!data.hasLearned(player.getUUID(), skillPath)) {
            CorpseOrigin.LOGGER.warn("玩家 {} 尝试激活未学习的技能: {}",
                    player.getName().getString(), skillPath);
            return false;
        }

        // 校验冷却
        long now = System.currentTimeMillis();
        long end = getCooldownEnd(player.getUUID(), skillPath);
        if (end > now) {
            return false;  // 仍在冷却中
        }

        // 执行效果
        skill.onActivate(player);

        // 写冷却
        int ticks = skill.getCooldownTicks();
        long newEnd = now + ticks * 50L;
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(skillPath, newEnd);

        // 同步冷却给客户端
        CorpseNetwork.sendCooldownSync(player, skillPath, ticks);

        CorpseOrigin.LOGGER.info("玩家 {} 激活技能 {}，冷却 {} ticks",
                player.getName().getString(), skillPath, ticks);
        return true;
    }

    /**
     * 学习技能（服务端，含校验 + 扣点）。
     *
     * @return true 表示成功学习
     */
    public static boolean learn(ServerPlayer player, String skillPath) {
        if (skillPath == null || skillPath.isEmpty()) {
            return false;
        }

        // 1. 找到技能定义
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        ISkill skill = null;
        for (ISkill s : character.getSkills()) {
            if (s.getId().getPath().equals(skillPath)) {
                skill = s;
                break;
            }
        }
        if (skill == null) {
            CorpseOrigin.LOGGER.warn("玩家 {} 尝试学习不存在的技能: {}",
                    player.getName().getString(), skillPath);
            return false;
        }

        // 2. 已学则直接返回
        PlayerCharacterData data = PlayerCharacterData.get(player);
        if (data.hasLearned(player.getUUID(), skillPath)) {
            return true;
        }

        // 3. 检查前置技能
        for (Identifier prereq : skill.getPrerequisites()) {
            if (!data.hasLearned(player.getUUID(), prereq.getPath())) {
                CorpseOrigin.LOGGER.warn("玩家 {} 学习 {} 前置未满足: {}",
                        player.getName().getString(), skillPath, prereq);
                return false;
            }
        }

        // 4. 检查进化等级和可用点数
        int level = EvolutionManager.getLevel(data.getEarnedPoints(player.getUUID()));
        if (level < skill.getRequiredLevel()) {
            return false;
        }
        if (data.getAvailablePoints(player.getUUID()) < skill.getCost()) {
            return false;
        }

        // 5. 扣点 + 学习
        data.spendPoints(player.getUUID(), skill.getCost());
        data.learnSkill(player.getUUID(), skillPath);

        // 6. 同步给客户端
        CorpseNetwork.sendEvolutionSync(player);

        CorpseOrigin.LOGGER.info("玩家 {} 学习了技能 {}", player.getName().getString(), skillPath);
        return true;
    }

    /**
     * 作弊解锁：跳过进化点、等级与前置，直接学会当前角色的全部技能。
     *
     * @return 授予的技能数量
     */
    public static int grantAllSkills(ServerPlayer player) {
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        PlayerCharacterData data = PlayerCharacterData.get(player);

        int granted = 0;
        for (ISkill skill : character.getSkills()) {
            data.learnSkill(player.getUUID(), skill.getId().getPath());
            granted++;
        }

        if (granted > 0) {
            CorpseNetwork.sendEvolutionSync(player);
        }

        CorpseOrigin.LOGGER.info("玩家 {} 作弊解锁了 {} 的全部技能（{} 个）",
                player.getName().getString(), character.getId(), granted);
        return granted;
    }

    private static long getCooldownEnd(UUID uuid, String skillPath) {
        Map<String, Long> map = COOLDOWNS.get(uuid);
        return map == null ? 0L : map.getOrDefault(skillPath, 0L);
    }

    // ==================== 冷却随身体保存 ====================

    /** 导出当前剩余冷却（技能路径 → 剩余 tick），用于跟着身体一起保存 */
    public static Map<String, Integer> snapshotRemaining(ServerPlayer player) {
        Map<String, Long> map = COOLDOWNS.get(player.getUUID());
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        long now = System.currentTimeMillis();
        Map<String, Integer> result = new HashMap<>();
        map.forEach((skillPath, endTime) -> {
            long remainingMs = endTime - now;
            if (remainingMs > 0) {
                result.put(skillPath, (int) Math.max(1L, (remainingMs + 49L) / 50L));
            }
        });
        return result;
    }

    /** 用给定剩余冷却覆盖玩家当前冷却（换身体时用）；空表即清空 */
    public static void restoreRemaining(ServerPlayer player, Map<String, Integer> remaining) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = new HashMap<>();
        remaining.forEach((skillPath, ticks) -> {
            if (ticks > 0) {
                map.put(skillPath, now + ticks * 50L);
            }
        });
        if (map.isEmpty()) {
            COOLDOWNS.remove(player.getUUID());
        } else {
            COOLDOWNS.put(player.getUUID(), map);
        }
    }

    /** 玩家断开连接时清理其冷却缓存 */
    public static void cleanupDisconnect(UUID uuid) {
        COOLDOWNS.remove(uuid);
    }
}
