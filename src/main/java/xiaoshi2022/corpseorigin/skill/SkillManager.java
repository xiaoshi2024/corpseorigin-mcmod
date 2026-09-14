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

    private static long getCooldownEnd(UUID uuid, String skillPath) {
        Map<String, Long> map = COOLDOWNS.get(uuid);
        return map == null ? 0L : map.getOrDefault(skillPath, 0L);
    }

    /** 玩家断开连接时清理其冷却缓存 */
    public static void cleanupDisconnect(UUID uuid) {
        COOLDOWNS.remove(uuid);
    }
}
