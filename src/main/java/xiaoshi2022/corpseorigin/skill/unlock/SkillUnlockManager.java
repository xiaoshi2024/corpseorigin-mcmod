package xiaoshi2022.corpseorigin.skill.unlock;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取式解锁的执行者。
 * <p>
 * 定期（以及玩家获得获取物时立即）检查：哪些技能声明了解锁来源、条件已满足、但还没学会，
 * 就把它们直接写进已学列表并提示玩家。整个过程<b>幂等</b>，重复调用不会重复解锁或重复提示。
 * <p>
 * 与技能树的「双通道」关系：{@code SkillManager.learn} 那条路（前置 + 进化等级 + 花点）完全不变；
 * 这里只负责另一条路（拿到东西 → 免费学会）。两条路的终点都是 {@code PlayerCharacterData.learnSkill}。
 */
public final class SkillUnlockManager {

    /** 定期扫描间隔（tick）。20 = 每秒一次，够快又几乎没有开销 */
    private static final int SCAN_INTERVAL_TICKS = 20;

    private SkillUnlockManager() {
    }

    /** 服务端 tick 入口：按间隔定期扫描（宠物、背包这类会自然变化的来源靠它兜底） */
    public static void tick(ServerPlayer player) {
        applyBlackGoldHeartInfection(player);
        if (player.tickCount % SCAN_INTERVAL_TICKS == 0) {
            grantUnlocked(player, false);
        }
    }

    /**
     * 植入黑金心脏之后按"尸兄身体"收尾：感染度顶到 100、恢复意识。
     * <p>
     * 黑小飞是在这一步从人类转为尸兄；本来就是尸兄的（见 {@code HeartImplant.canBear}）
     * 只是补一次感染度，不会重复转换。
     */
    private static void applyBlackGoldHeartInfection(ServerPlayer player) {
        if (player.tickCount % SCAN_INTERVAL_TICKS != 0
                || !xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant.canBear(player)
                || !xiaoshi2022.corpseorigin.component.PlayerRelicComponent.has(player,
                    xiaoshi2022.corpseorigin.skill.heixiaofei.BlackGoldHeartSkill.RELIC_ID)) return;
        var corpse = xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.get(player);
        boolean changed = !corpse.isCorpse() || corpse.getInfection() != 100;
        if (!corpse.isCorpse()) {
            xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.setPlayerAsCorpse(player,
                    xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.TYPE_ELITE,
                    xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);
            corpse = xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.get(player);
        }
        corpse.setInfection(100);
        corpse.restoreConsciousness();
        if (changed) {
            CorpseNetwork.sendInfectionSync(player);
            CorpseNetwork.broadcastPlayerCorpseSync(player);
            player.sendOverlayMessage(Component.translatable("character.corpseorigin.heixiaofei.black_gold_infected"));
        }
    }

    /**
     * 立即检查并免费学会所有条件已满足的技能。
     *
     * @param quiet true 时静默解锁（批量授予器官时用，避免刷屏）
     * @return 本次新学会的技能数量
     */
    public static int grantUnlocked(ServerPlayer player, boolean quiet) {
        if (!player.isAlive() || player.isSpectator()) return 0;
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        PlayerCharacterData data = PlayerCharacterData.get(player);

        List<ISkill> candidates = new ArrayList<>(character.getSkills());
        // 固定角色也能靠「获取物」学到别的角色的招式（形态 / 身体改造类除外，见 SkillLearningRules.crossLearnable）。
        // 凡人 / 尸兄的 getSkills() 本来就是全体角色的并集，下面的去重保证不会重复处理。
        for (ISkill skill : xiaoshi2022.corpseorigin.growth.FreeGrowth.skills()) {
            if (!SkillLearningRules.crossLearnable(skill)) continue;
            if (candidates.stream().anyMatch(s -> s.getId().getPath().equals(skill.getId().getPath()))) continue;
            candidates.add(skill);
        }

        List<ISkill> newly = new ArrayList<>();
        for (ISkill skill : candidates) {
            // 自由路线学满就不再多给。这条扫描每 20 tick 跑一次，所以这里<b>不</b>刷提示，
            // 只在玩家主动学（SkillManager.learn）或拜师时提示，免得每秒弹一次。
            if (xiaoshi2022.corpseorigin.growth.FreeGrowth.skillLimitReached(player)) break;
            if (xiaoshi2022.corpseorigin.growth.WeaponEligibility.skillReason(player,skill.getId().getPath())!=null) continue;
            boolean innate = SkillLearningRules.innate(character.getId(), skill.getId().getPath());
            if (!innate && skill.getUnlockSources().isEmpty()) {
                continue;   // 纯技能树技能，不走这条路
            }
            if (data.hasLearned(player.getUUID(), skill.getId().getPath())) {
                continue;
            }
            if (!innate && !isSatisfied(player, skill)) {
                continue;
            }
            data.learnSkill(player.getUUID(), skill.getId().getPath());
            newly.add(skill);
        }

        if (newly.isEmpty()) {
            return 0;
        }

        if (!quiet) {
            for (ISkill skill : newly) {
                // actionbar 提示：玩家抓到获取物的瞬间就能看到反馈
                player.sendOverlayMessage(Component.translatable(
                        "skill.corpseorigin.unlocked_by_source", skill.getName()));
            }
        }
        CorpseNetwork.sendEvolutionSync(player);

        CorpseOrigin.LOGGER.info("玩家 {} 通过获得物解锁了 {} 个技能: {}",
                player.getName().getString(), newly.size(),
                newly.stream().map(s -> s.getId().getPath()).toList());
        return newly.size();
    }

    /** 某技能的解锁条件是否已全部满足（多个来源之间是「或」的关系） */
    public static boolean isSatisfied(ServerPlayer player, ISkill skill) {
        if (xiaoshi2022.corpseorigin.growth.WeaponEligibility.skillReason(player,skill.getId().getPath())!=null) return false;
        for (SkillUnlockSource source : skill.getUnlockSources()) {
            if (source.id().startsWith("character:") && xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(player)
                    && xiaoshi2022.corpseorigin.growth.FreeGrowth.discovered(player,skill.getId().getPath())) return true;
            if (source.isSatisfiedBy(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 某技能还没满足的解锁条件 —— 给指令与技能界面显示「缺什么」。
     *
     * @return 条件列表；空表示这个技能没有获取式来源（纯技能树技能）
     */
    public static List<SkillUnlockSource> unmetSources(ServerPlayer player, ISkill skill) {
        List<SkillUnlockSource> unmet = new ArrayList<>();
        for (SkillUnlockSource source : skill.getUnlockSources()) {
            if (!source.isSatisfiedBy(player)) {
                unmet.add(source);
            }
        }
        return unmet;
    }

    /** 玩家当前角色里，所有「有获取式来源但尚未解锁」的技能 */
    public static List<ISkill> pending(ServerPlayer player) {
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        PlayerCharacterData data = PlayerCharacterData.get(player);

        List<ISkill> result = new ArrayList<>();
        for (ISkill skill : character.getSkills()) {
            if (skill.getUnlockSources().isEmpty()) {
                continue;
            }
            if (data.hasLearned(player.getUUID(), skill.getId().getPath())) {
                continue;
            }
            result.add(skill);
        }
        return result;
    }
}
