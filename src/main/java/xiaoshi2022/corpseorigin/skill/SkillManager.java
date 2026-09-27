package xiaoshi2022.corpseorigin.skill;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
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

    /** 激活技能（服务端，含校验）：要求已学会，与 {@link #activate(ServerPlayer, String, boolean)} 等价。 */
    public static boolean activate(ServerPlayer player, String skillPath) {
        return activate(player, skillPath, true);
    }

    /**
     * 激活技能（服务端，含校验）。
     *
     * @param requireLearned {@code false} = <b>不校验"是否已学会"</b>。
     *                       给"握着兵器就是钥匙"的招式用（见 {@code RoleChapterSkill#castWithWeapon}）：
     *                       兵器在手就能使，不必先去技能树点亮。
     * @return true 表示成功激活并发送冷却同步包
     */
    public static boolean activate(ServerPlayer player, String skillPath, boolean requireLearned) {
        if (xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill.PATH.equals(skillPath)
                && xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill.isChanneling(player)) {
            xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill.cancel(player);
            return true;
        }
        if (xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant.active(player)) return false;
        if(!player.isAlive() || player.isSpectator())return false;
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
        // 跨角色「获取式」招式：固定角色靠拿到东西 / 遇到指定事物学会的外角色技能
        //（形态 / 身体改造类排除在外，见 SkillLearningRules.crossLearnable）
        if (skill == null) {
            skill = xiaoshi2022.corpseorigin.growth.FreeGrowth.skills().stream()
                    .filter(xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules::crossLearnable)
                    .filter(s -> s.getId().getPath().equals(skillPath))
                    .findFirst().orElse(null);
        }
// ★ 动态技能：少教主进入不死髅体后临时获得的躯体技能
        if (skill == null) {
            skill = xiaoshi2022.corpseorigin.skill.longyou.UndeadBodyState
                    .getDynamicSkill(player, skillPath);
        }
        if (skill == null) {
            CorpseOrigin.LOGGER.warn("玩家 {} 尝试激活不存在的技能: {}",
                    player.getName().getString(), skillPath);
            return false;
        }
        if (!skill.isActivatable()) {
            return false;
        }

        // 校验已学习（「原体保底」例外见 isOriginalBodyFallback；兵器招式由调用方声明免检）
        PlayerCharacterData data = PlayerCharacterData.get(player);
        if (requireLearned && !data.hasLearned(player.getUUID(), skillPath)
                && !isOriginalBodyFallback(player, skillPath)
                && xiaoshi2022.corpseorigin.skill.longyou.UndeadBodyState.getDynamicSkill(player, skillPath) == null) {
            CorpseOrigin.LOGGER.warn("玩家 {} 尝试激活未学习的技能: {}",
                    player.getName().getString(), skillPath);
            return false;
        }

        // 校验冷却
        // Existing constructs accept follow-up input without restarting the summon/launch cooldown.
        if(skillPath.equals("tiger_claw_bee_wheel") && player.getMainHandItem().getItem() instanceof xiaoshi2022.corpseorigin.item.BeeWheelItem){
            var wheel=xiaoshi2022.corpseorigin.entity.BeeWheelEntity.active(player);
            if(wheel!=null){wheel.control(player);return true;}
        }
        if(skillPath.equals("killing_incarnation")){
            var incarnation=xiaoshi2022.corpseorigin.entity.SkillConstructEntity.findOwned(player,"slaughter_incarnation");
            if(incarnation!=null)return incarnation.activateSpecial(player);
        }
        // Release controls remain available even when the corresponding resource is empty.
        if (isRelease(player, skillPath)) {
            skill.onActivate(player);
            return true;
        }
        long now = System.currentTimeMillis();
        long end = getCooldownEnd(player.getUUID(), skillPath);
        if (end > now) {
            return false;  // 仍在冷却中
        }

        // 硬前置（形态 / 宠物在身边之类）：不满足就只提示，不吃冷却、不扣内力
        var eligibility=xiaoshi2022.corpseorigin.growth.WeaponEligibility.skillReason(player,skillPath);
        if(eligibility!=null){player.sendOverlayMessage(eligibility);return false;}
        net.minecraft.network.chat.Component blocked = skill.checkUsable(player);
        if (blocked != null) {
            player.sendOverlayMessage(blocked);
            return false;
        }

        if (!SkillResources.pay(player, skill.getResourceCost())) return false;

        if (!skillPath.equals("gourd_mortal_disguise") && !skillPath.equals("gourd_inheritance_cycle")
                && !skillPath.equals("gourd_inheritance"))
            xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.reveal(player);
        skill.onActivate(player);

        // 写冷却
        int ticks = skill.getCooldownTicks();
        long newEnd = now + ticks * 50L;
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(skillPath, newEnd);

        // 同步冷却给客户端
        CorpseNetwork.sendCooldownSync(player, skillPath, ticks);

        CorpseOrigin.LOGGER.debug("玩家 {} 激活技能 {}，冷却 {} ticks",
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

        // 2.5 自由路线（凡人 / 尸兄）学满上限就不再收新技能 —— 已学会的照旧能重放（上面已提前返回）
        if (xiaoshi2022.corpseorigin.growth.FreeGrowth.skillLimitReached(player)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "message.corpseorigin.free_growth.skill_limit",
                    xiaoshi2022.corpseorigin.growth.FreeGrowth.SKILL_LIMIT));
            return false;
        }

        // Innate/item/encounter skills never charge points or bypass their source requirements.
        var eligibility=xiaoshi2022.corpseorigin.growth.WeaponEligibility.skillReason(player,skillPath);
        if(eligibility!=null){player.sendOverlayMessage(eligibility);return false;}
        if (xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.innate(character.getId(), skillPath)
                || !skill.getUnlockSources().isEmpty()) {
            xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockManager.grantUnlocked(player, false);
            return data.hasLearned(player.getUUID(), skillPath);
        }

        // 3. 检查前置技能
        if (xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(player)
                && !xiaoshi2022.corpseorigin.growth.FreeGrowth.discovered(player,skillPath)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.skill_manager.text_01"));
            return false;
        }
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

    /**
     * 「原体保底」：人缩在尸王原体里的时候，「血肉重塑」不校验"是否已学会"。
     * <p>
     * 否则只要没在技能树里点过它，玩家就会被困在拇指大小的身体里出不来。
     * 这一条是硬保底 —— 不依赖"进原体时自动学会"那条路径，老存档 / 异常途径也能救回来。
     */
    private static boolean isOriginalBodyFallback(ServerPlayer player, String skillPath) {
        return xiaoshi2022.corpseorigin.skill.longyou.FleshReshapeSkill.PATH.equals(skillPath)
                && xiaoshi2022.corpseorigin.skill.longyou.BodyTransplantHandler.isInOriginalBody(player);
    }

    private static boolean isRelease(ServerPlayer player, String path) {
        return switch (path) {
            case "ancient_poetry_sword" -> xiaoshi2022.corpseorigin.skill.baixiaofei.AncientPoetrySwordSkill.isRunning(player);
            case "thunder_power" -> xiaoshi2022.corpseorigin.skill.longyou.ThunderPowerSkill.isEnabled(player.getUUID());
            case "xuanwu_body" -> player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.longyou.UndeadBodyState.STATE) == 1;
            case "son_of_corpse_nest" -> xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.get(player).getVariant()
                    == xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.VARIANT_SHICHAOZHIZI;
            default -> false;
        };
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
