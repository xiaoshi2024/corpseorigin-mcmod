package xiaoshi2022.corpseorigin.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 骨架技能基类 —— 为尚未实装的技能统一提供样板实现（id / 名称 / 类型 / 冷却 / 消耗）。
 * <p>
 * 子类通常只需要：
 * <ol>
 *   <li>在构造函数里传入 id、技能类型、冷却等元数据；</li>
 *   <li>用 javadoc 写清这个技能「打算做成什么样」；</li>
 *   <li>后续实装时重写 {@link #onActivate}。</li>
 * </ol>
 * i18n key 约定：{@code skill.corpseorigin.<path>} 与 {@code skill.corpseorigin.<path>.desc}。
 */
public abstract class AbstractSkill implements ISkill {

    private final String path;
    private final SkillType type;
    private final int cooldownTicks;
    private final int cost;
    private final int requiredLevel;
    private final boolean activatable;
    private final int innerPowerCost;

    protected AbstractSkill(String path, SkillType type, int cooldownTicks,
                            int cost, int requiredLevel, boolean activatable, int innerPowerCost) {
        this.path = path;
        this.type = type;
        this.cooldownTicks = cooldownTicks;
        this.cost = cost;
        this.requiredLevel = requiredLevel;
        this.activatable = activatable;
        this.innerPowerCost = innerPowerCost;
    }

    /** 主动技能（点数 / 等级由 SkillLearningRules 按类型与冷却推导，不消耗内力） */
    protected AbstractSkill(String path, SkillType type, int cooldownTicks) {
        this(path, type, cooldownTicks,
                xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.cost(path, type, cooldownTicks, true),
                xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.level(path, type, cooldownTicks, true), true, 0);
    }

    /** 主动技能（指定内力消耗） */
    protected AbstractSkill(String path, SkillType type, int cooldownTicks, int innerPowerCost) {
        this(path, type, cooldownTicks,
                xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.cost(path, type, cooldownTicks, true),
                xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.level(path, type, cooldownTicks, true), true, innerPowerCost);
    }

    /** 被动技能（不进技能轮盘、无冷却） */
    protected AbstractSkill(String path, SkillType type) {
        this(path, type, 0,
                xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.cost(path, type, 0, false),
                xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules.level(path, type, 0, false), false, 0);
    }

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, path);
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin." + path);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin." + path + ".desc");
    }

    @Override
    public SkillType getSkillType() {
        return type;
    }

    @Override
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public int getCost() {
        return getUnlockSources().isEmpty() ? cost : 0;
    }

    @Override
    public java.util.List<xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource> getUnlockSources() {
        return xiaoshi2022.corpseorigin.skill.unlock.ItemSkillSources.forSkill(path);
    }

    @Override
    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public boolean isActivatable() {
        return activatable;
    }

    @Override
    public int getInnerPowerCost() {
        return innerPowerCost;
    }

    /**
     * 技能效果：骨架阶段为空实现。
     * <p>
     * TODO 实装技能效果（伤害/状态/召唤/粒子表现等）。
     */
    @Override
    public void onActivate(net.minecraft.server.level.ServerPlayer player) {
        // 骨架技能，效果待实装
    }
}
