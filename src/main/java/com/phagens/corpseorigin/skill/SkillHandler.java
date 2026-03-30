package com.phagens.corpseorigin.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.SyncSkillDataPacket;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * 技能处理器实现 - 管理玩家的技能状态
 */
public class SkillHandler implements ISkillHandler {

    private static final String NBT_LEARNED_SKILLS = "learned_skills";
    private static final String NBT_EVOLUTION_POINTS = "evolution_points";
    private static final String NBT_COOLDOWNS = "cooldowns";
    private static final String NBT_ACTIVE_SKILLS = "active_skills";

    // 添加 Codec 用于序列化
    public static final Codec<SkillHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(ResourceLocation.CODEC).fieldOf(NBT_LEARNED_SKILLS).forGetter(h -> new ArrayList<>(h.learnedSkills)),
            Codec.INT.fieldOf(NBT_EVOLUTION_POINTS).forGetter(h -> h.evolutionPoints),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).fieldOf(NBT_COOLDOWNS).forGetter(h -> new HashMap<>(h.cooldowns)),
            Codec.list(ResourceLocation.CODEC).fieldOf(NBT_ACTIVE_SKILLS).forGetter(h -> new ArrayList<>(h.activeSkills))
    ).apply(instance, (learned, points, cds, active) -> {
        SkillHandler handler = new SkillHandler(null);
        handler.learnedSkills.addAll(learned);
        handler.evolutionPoints = points;
        handler.cooldowns.putAll(cds);
        handler.activeSkills.addAll(active);

        CorpseOrigin.LOGGER.info("【反序列化】加载技能数据: {} 个技能, {} 进化点", learned.size(), points);
        return handler;
    }));

    private Player player;
    private final Set<ResourceLocation> learnedSkills = new HashSet<>();
    private int evolutionPoints = 0;
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private final Set<ResourceLocation> activeSkills = new HashSet<>();

    private boolean dirty = false;

    // 用于反序列化的构造函数
    private SkillHandler(List<ResourceLocation> learnedList, int points,
                         Map<ResourceLocation, Integer> cds, List<ResourceLocation> activeList) {
        this.learnedSkills.addAll(learnedList);
        this.evolutionPoints = points;
        this.cooldowns.putAll(cds);
        this.activeSkills.addAll(activeList);
        this.player = null;

        // 【调试】打印反序列化数据
        CorpseOrigin.LOGGER.info("【反序列化】创建 SkillHandler，技能数量: {}, 进化点: {}",
                learnedList.size(), points);
        if (!learnedList.isEmpty()) {
            for (ResourceLocation id : learnedList) {
                CorpseOrigin.LOGGER.info("  - 已学习技能: {}", id);
            }
        }
    }

    public SkillHandler(Player player) {
        this.player = player;

        if (player != null) {
            CorpseOrigin.LOGGER.info("【新创建】SkillHandler for player: {}, 当前无技能数据",
                    player.getName().getString());
        } else {
            CorpseOrigin.LOGGER.info("【新创建】SkillHandler for player: null (等待反序列化)");
        }
    }

    /**
     * 设置玩家引用（用于反序列化后）
     */
    public void setPlayer(Player player) {
        if (this.player == null && player != null) {
            this.player = player;

            CorpseOrigin.LOGGER.info("【设置玩家】Player: {}, 当前技能数量: {}, 进化点: {}",
                    player.getName().getString(), learnedSkills.size(), evolutionPoints);

            // 如果已有技能，重新应用被动效果
            if (!learnedSkills.isEmpty() && !player.level().isClientSide) {
                reapplyPassiveSkills();
            }
        } else if (this.player != player) {
            // 这种情况不应该发生，但以防万一
            CorpseOrigin.LOGGER.warn("尝试重新设置玩家引用，忽略");
        }
    }

    /**
     * 获取或创建玩家的技能处理器
     */
    public static ISkillHandler getOrCreate(Player player) {
        return player.getData(SkillAttachment.SKILL_HANDLER);
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public  LearnResult getNbtLearnedSkills(ResourceLocation skillId) {
        ISkill skill = SkillManager.getInstance().getSkill(skillId);
        if (skill == null) {
            return LearnResult.UNKNOWN_SKILL;
        }
        return learnSkill(skill);
    }


    /**
     * 学习功法技能（绕过尸兄检查）
     * 只用于功法系统，不需要进化点，不需要尸兄身份
     */
    public LearnResult learnGongFuSkill(ResourceLocation skillId) {
        ISkill skill = SkillManager.getInstance().getSkill(skillId);
        if (skill == null) {
            return LearnResult.UNKNOWN_SKILL;
        }

        if (hasLearned(skill)) {
            return LearnResult.ALREADY_LEARNED;
        }

        // ⭐ 不检查 isCorpse，允许普通人学习功法技能

        // 不检查等级、进化点、前置技能等
        // 功法技能只要有功法物品就能学

        try {
            learnedSkills.add(skill.getId());

            dirty = true;
            syncToClient();

            CorpseOrigin.LOGGER.info("玩家 {} 学习功法技能 {} 成功",
                    player.getName().getString(), skill.getId());

            return LearnResult.SUCCESS;
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("学习功法技能 {} 时发生错误", skill.getId(), e);
            return LearnResult.ERROR;
        }
    }

    /**
     * 同步技能数据到客户端
     */
    @Override
    public void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            // 即使不脏也强制同步，确保客户端数据正确
            SyncSkillDataPacket packet = new SyncSkillDataPacket(
                    player.getId(),
                    new ArrayList<>(learnedSkills),
                    evolutionPoints,
                    new HashMap<>(cooldowns)
            );

//            CorpseOrigin.LOGGER.debug("【同步到客户端】玩家 {}: {} 个技能, {} 进化点",
//                    player.getName().getString(), learnedSkills.size(), evolutionPoints);

            PacketDistributor.sendToPlayer(serverPlayer, packet);
            dirty = false;
        }
    }

    /**
     * 强制同步所有数据到客户端
     */
    public void forceSyncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            SyncSkillDataPacket packet = new SyncSkillDataPacket(
                    player.getId(),
                    new ArrayList<>(learnedSkills),
                    evolutionPoints,
                    new HashMap<>(cooldowns)
            );

            CorpseOrigin.LOGGER.debug("【强制同步】玩家{}: {} 个技能, {} 进化点",
                    player.getName().getString(), learnedSkills.size(), evolutionPoints);

            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    @Override
    public Set<ISkill> getLearnedSkills() {
        Set<ISkill> skills = new HashSet<>();
        for (ResourceLocation id : learnedSkills) {
            ISkill skill = SkillManager.getInstance().getSkill(id);
            if (skill != null) {
                skills.add(skill);
            } else {
                CorpseOrigin.LOGGER.warn("玩家 {} 学习的技能 {} 不存在",
                        player != null ? player.getName().getString() : "unknown", id);
            }
        }
        return Collections.unmodifiableSet(skills);
    }

    @Override
    public Set<ResourceLocation> getLearnedSkillIds() {
        return Collections.unmodifiableSet(learnedSkills);
    }

    @Override
    public boolean hasLearned(ISkill skill) {
        return learnedSkills.contains(skill.getId());
    }

    @Override
    public boolean hasLearned(ResourceLocation skillId) {
        return learnedSkills.contains(skillId);
    }

    @Override
    public LearnResult learnSkill(ISkill skill) {
        if (skill == null) {
            return LearnResult.UNKNOWN_SKILL;
        }

        if (hasLearned(skill)) {
            return LearnResult.ALREADY_LEARNED;
        }

        if (!PlayerCorpseData.isCorpse(player)) {
            return LearnResult.NOT_CORPSE;
        }

        int playerLevel = PlayerCorpseData.getEvolutionLevel(player);
        if (playerLevel < skill.getRequiredLevel()) {
            return LearnResult.LEVEL_TOO_LOW;
        }

        for (ResourceLocation prereq : skill.getPrerequisites()) {
            if (!hasLearned(prereq)) {
                return LearnResult.MISSING_PREREQUISITES;
            }
        }

        if (evolutionPoints < skill.getCost()) {
            return LearnResult.INSUFFICIENT_POINTS;
        }

        for (ISkill learnedSkill : getLearnedSkills()) {
            if (isMutuallyExclusive(skill, learnedSkill)) {
                return LearnResult.MUTUALLY_EXCLUSIVE;
            }
        }

        try {
            learnedSkills.add(skill.getId());
            evolutionPoints -= skill.getCost();

            // 注意：这里不再调用 onLearn，因为属性会在下次登录时重新应用
            // 或者如果你需要立即应用，确保先移除再添加

            if (skill.isPassive()) {
                activeSkills.add(skill.getId());
            }

            dirty = true;
            syncToClient();

            CorpseOrigin.LOGGER.info("玩家 {} 学习技能 {} 成功，剩余进化点: {}",
                    player.getName().getString(), skill.getId(), evolutionPoints);

            return LearnResult.SUCCESS;
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("学习技能 {} 时发生错误", skill.getId(), e);
            return LearnResult.ERROR;
        }
    }

    /**
     * 重新应用所有被动技能（登录时调用）
     */
    public void reapplyPassiveSkills() {
        if (player == null) return;

        for (ResourceLocation skillId : learnedSkills) {
            ISkill skill = SkillManager.getInstance().getSkill(skillId);
            if (skill != null && skill.isPassive()) {
                // 先移除再添加，避免重复
                if (skill instanceof BaseSkill baseSkill) {
                    baseSkill.removeAttributeModifiers(player);
                    baseSkill.applyAttributeModifiers(player);
                } else {
                    skill.onLearn(player);
                }
            }
        }
        CorpseOrigin.LOGGER.info("重新应用 {} 个被动技能到玩家 {}", learnedSkills.size(), player.getName().getString());
    }

    @Override
    public LearnResult learnSkill(ResourceLocation skillId) {
        ISkill skill = SkillManager.getInstance().getSkill(skillId);
        if (skill == null) {
            return LearnResult.UNKNOWN_SKILL;
        }
        return learnSkill(skill);
    }

    @Override
    public boolean forgetSkill(ISkill skill) {
        if (skill == null || !hasLearned(skill)) {
            return false;
        }

        // 返还进化点数
        learnedSkills.remove(skill.getId());
        evolutionPoints += skill.getCost();

        // 如果技能处于激活状态，停用它
        if (activeSkills.contains(skill.getId())) {
            activeSkills.remove(skill.getId());
        }

        // 移除冷却
        cooldowns.remove(skill.getId());

        skill.onForget(player);

        dirty = true;
        syncToClient();

        CorpseOrigin.LOGGER.debug("玩家 {} 遗忘了技能 {}", player.getName().getString(), skill.getId());
        return true;
    }

    @Override
    public boolean forgetSkill(ResourceLocation skillId) {
        ISkill skill = SkillManager.getInstance().getSkill(skillId);
        if (skill == null) {
            return false;
        }
        return forgetSkill(skill);
    }

    @Override
    public void forgetAllSkills() {
        // 返还所有进化点数
        for (ISkill skill : getLearnedSkills()) {
            evolutionPoints += skill.getCost();
            skill.onForget(player);
        }

        learnedSkills.clear();
        activeSkills.clear();
        cooldowns.clear();

        dirty = true;
        syncToClient();

        CorpseOrigin.LOGGER.debug("玩家 {} 遗忘了所有技能", player.getName().getString());
    }

    @Override
    public boolean canLearn(ISkill skill) {
        if (skill == null) return false;
        if (hasLearned(skill)) return false;
        if (!PlayerCorpseData.isCorpse(player)) return false;
        if (PlayerCorpseData.getEvolutionLevel(player) < skill.getRequiredLevel()) return false;
        if (evolutionPoints < skill.getCost()) return false;

        // 检查前置技能
        for (ResourceLocation prereq : skill.getPrerequisites()) {
            if (!hasLearned(prereq)) return false;
        }

        // 检查互斥
        for (ISkill learnedSkill : getLearnedSkills()) {
            if (isMutuallyExclusive(skill, learnedSkill)) return false;
        }

        return true;
    }

    @Override
    public int getEvolutionPoints() {
        return evolutionPoints;
    }

    @Override
    public int getUsedEvolutionPoints() {
        int used = 0;
        for (ISkill skill : getLearnedSkills()) {
            used += skill.getCost();
        }
        return used;
    }

    @Override
    public int getTotalEvolutionPoints() {
        return evolutionPoints + getUsedEvolutionPoints();
    }

    @Override
    public void addEvolutionPoints(int points) {
        if (points > 0) {
            evolutionPoints += points;
            dirty = true;
            syncToClient();
            CorpseOrigin.LOGGER.info("玩家 {} 获得 {} 进化点数，当前总点数: {}",
                    player.getName().getString(), points, evolutionPoints);
        }
    }

    @Override
    public boolean consumeEvolutionPoints(int points) {
        if (points <= 0) return true;
        if (evolutionPoints >= points) {
            evolutionPoints -= points;
            dirty = true;
            syncToClient();
            return true;
        }
        return false;
    }

    @Override
    public void setEvolutionPoints(int points) {
        this.evolutionPoints = Math.max(0, points);
        dirty = true;
        syncToClient();
    }

    @Override
    public List<ISkill> getActiveSkills() {
        List<ISkill> skills = new ArrayList<>();
        for (ResourceLocation id : activeSkills) {
            ISkill skill = SkillManager.getInstance().getSkill(id);
            if (skill != null) {
                skills.add(skill);
            }
        }
        return Collections.unmodifiableList(skills);
    }

    @Override
    public boolean activateSkill(ISkill skill) {
        if (skill == null || !hasLearned(skill)) {
            return false;
        }

        // 检查冷却
        if (isOnCooldown(skill)) {
            return false;
        }

        // 激活技能
        skill.onActivate(player);

        // 设置冷却
        if (skill.getCooldown() > 0) {
            setCooldown(skill, skill.getCooldown());
        }

        // 如果是持续技能，添加到激活列表
        if (skill.getDuration() > 0) {
            activeSkills.add(skill.getId());
        }

        dirty = true;
        return true;
    }

    @Override
    public boolean activateSkill(ResourceLocation skillId) {
        ISkill skill = SkillManager.getInstance().getSkill(skillId);
        if (skill == null) {
            return false;
        }
        return activateSkill(skill);
    }

    @Override
    public int getCooldownRemaining(ISkill skill) {
        return cooldowns.getOrDefault(skill.getId(), 0);
    }

    @Override
    public int getCooldownRemaining(ResourceLocation skillId) {
        return cooldowns.getOrDefault(skillId, 0);
    }

    @Override
    public void setCooldown(ISkill skill, int cooldown) {
        if (cooldown > 0) {
            cooldowns.put(skill.getId(), cooldown);
        } else {
            cooldowns.remove(skill.getId());
        }
        dirty = true;
    }

    @Override
    public boolean isOnCooldown(ISkill skill) {
        return cooldowns.getOrDefault(skill.getId(), 0) > 0;
    }

    @Override
    public String getCannotLearnReason(ISkill skill) {
        LearnResult result = learnSkill(skill);
        return result.getMessage();
    }

    /**
     * 从同步数据加载（客户端使用）
     */
    public void loadFromSyncData(List<ResourceLocation> skills, int points, Map<ResourceLocation, Integer> cds) {
        // ⭐ 正确的数据比较逻辑
        boolean skillsChanged = this.learnedSkills.size() != skills.size()
                || !this.learnedSkills.containsAll(skills);
        boolean pointsChanged = this.evolutionPoints != points;
        boolean cooldownsChanged = this.cooldowns.size() != cds.size()
                || !this.cooldowns.entrySet().containsAll(cds.entrySet());

        // 如果数据没变化，直接返回（避免重复应用被动技能）
        if (!skillsChanged && !pointsChanged && !cooldownsChanged) {
            return;
        }

//        CorpseOrigin.LOGGER.debug("loadFromSyncData - 数据发生变化，重新加载");
//        CorpseOrigin.LOGGER.debug("  - skillsChanged: {}, pointsChanged: {}, cooldownsChanged: {}",
//                skillsChanged, pointsChanged, cooldownsChanged);
        boolean needReapplyPassives = skillsChanged;
        // 清除现有数据
        learnedSkills.clear();
        cooldowns.clear();
        activeSkills.clear();

        // 加载新数据
        learnedSkills.addAll(skills);
        evolutionPoints = points;
        cooldowns.putAll(cds);

        // 自动将被动技能添加到激活列表
        for (ResourceLocation skillId : learnedSkills) {
            ISkill skill = SkillManager.getInstance().getSkill(skillId);
            if (skill != null && skill.isPassive()) {
                activeSkills.add(skillId);
            }
        }

//        CorpseOrigin.LOGGER.info("loadFromSyncData 完成 - 最终技能数量: {}, 进化点: {}",
//                learnedSkills.size(), evolutionPoints);

        if (needReapplyPassives && player != null) {
            reapplyPassiveSkillsInternal();
        }
    }

    private void reapplyPassiveSkillsInternal() {
        if (player == null) return;

        for (ResourceLocation skillId : learnedSkills) {
            ISkill skill = SkillManager.getInstance().getSkill(skillId);
            if (skill != null && skill.isPassive()) {
                // 先移除再添加，避免重复
                if (skill instanceof BaseSkill baseSkill) {
                    baseSkill.removeAttributeModifiers(player);
                    baseSkill.applyAttributeModifiers(player);
                } else {
                    skill.onLearn(player);
                }
            }
        }
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // 保存已学习技能
        ListTag skillsList = new ListTag();
        for (ResourceLocation skillId : learnedSkills) {
            skillsList.add(StringTag.valueOf(skillId.toString()));
        }
        tag.put(NBT_LEARNED_SKILLS, skillsList);

        // 保存进化点数
        tag.putInt(NBT_EVOLUTION_POINTS, evolutionPoints);

        CorpseOrigin.LOGGER.info("【保存到NBT】玩家 {} 保存技能数据: {} 个技能, {} 进化点",
                player != null ? player.getName().getString() : "未知",
                learnedSkills.size(), evolutionPoints);

        return tag;
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        learnedSkills.clear();

        if (tag.contains(NBT_LEARNED_SKILLS)) {
            ListTag skillsList = tag.getList(NBT_LEARNED_SKILLS, 8);
            for (int i = 0; i < skillsList.size(); i++) {
                String skillId = skillsList.getString(i);
                learnedSkills.add(ResourceLocation.parse(skillId));
            }
        }

        evolutionPoints = tag.getInt(NBT_EVOLUTION_POINTS);

        CorpseOrigin.LOGGER.info("【从NBT加载】玩家 {} 加载技能数据: {} 个技能, {} 进化点",
                player != null ? player.getName().getString() : "未知",
                learnedSkills.size(), evolutionPoints);

        this.dirty = true;
    }

    /**
     * 更新冷却（每tick调用）
     */
    public void updateCooldowns() {
        if (cooldowns.isEmpty()) {
            return;
        }
        boolean hasChanges = false;

        Iterator<Map.Entry<ResourceLocation, Integer>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry = iterator.next();
            int newCooldown = entry.getValue() - 1;

            if (newCooldown <= 0) {
                // CD 结束，移除
                iterator.remove();
                hasChanges = true;
            } else {
                // CD 进行中，更新时间
                entry.setValue(newCooldown);
                hasChanges = true;
            }
        }

        // 只有在 CD 实际变化时才同步
        if (hasChanges) {
            dirty = true;
            syncToClient();

        }
        }





    /**
     * 检查两个技能是否互斥
     */
    private boolean isMutuallyExclusive(ISkill skill1, ISkill skill2) {
        // 检查技能节点互斥
        for (ISkillNode node : SkillManager.getInstance().getAllNodes()) {
            if (node.containsSkill(skill1) && node.containsSkill(skill2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否需要同步到客户端
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * 强制标记为脏数据，触发同步
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * 标记为已同步
     */
    public void markClean() {
        dirty = false;
    }
}