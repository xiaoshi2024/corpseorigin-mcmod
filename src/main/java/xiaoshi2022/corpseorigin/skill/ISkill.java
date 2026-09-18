package xiaoshi2022.corpseorigin.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

import java.util.List;

/**
 * 技能接口 - 角色技能的核心抽象
 */
public interface ISkill {

    /** 技能唯一 ID */
    Identifier getId();

    /** 技能名称 */
    Component getName();

    /** 技能描述 */
    Component getDescription();

    /** 技能类型 */
    SkillType getSkillType();

    /** 学习消耗的进化点（技能树用） */
    default int getCost() {
        return 1;
    }

    /** 学习所需的最低进化等级（技能树用） */
    default int getRequiredLevel() {
        return 1;
    }

    /** 前置技能 ID（技能树用） */
    default List<Identifier> getPrerequisites() {
        return List.of();
    }

    /**
     * 获取式解锁来源（见 {@link xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource}）。
     * <p>
     * 默认空 = 纯技能树技能，只能花进化点点亮，行为和以前完全一致。
     * 声明了来源的技能，玩家满足条件时会被免费直接学会 —— 例如拿到「黑金心脏」这个器官，
     * 就自动解锁黑金心脏技能。两条路并存，互不冲突。
     * <p>
     * 多个来源之间的关系是「或」：满足任意一个就算解锁。
     */
    default List<SkillUnlockSource> getUnlockSources() {
        return List.of();
    }

    /** 是否为主动技能（可放入技能轮盘） */
    boolean isActivatable();

    /** 冷却时间（ticks） */
    int getCooldownTicks();

    /**
     * 内力消耗（0 = 不消耗）。
     * <p>
     * 只有拥有内力的角色（{@code getMaxInnerPower() > 0}）才会扣除内力；
     * 无内力角色使用内力消耗 > 0 的技能时，内力自动视为足够。
     */
    default int getInnerPowerCost() {
        return 0;
    }

    /** 服务端玩家激活技能时的效果回调 */
    default void onActivate(ServerPlayer player) {
        // 空实现，子类可重写
    }
}
