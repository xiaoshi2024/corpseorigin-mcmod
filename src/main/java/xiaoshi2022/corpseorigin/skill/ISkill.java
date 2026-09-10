package xiaoshi2022.corpseorigin.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

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

    /** 是否为主动技能（可放入技能轮盘） */
    boolean isActivatable();

    /** 冷却时间（ticks） */
    int getCooldownTicks();

    /** 服务端玩家激活技能时的效果回调 */
    default void onActivate(ServerPlayer player) {
        // 空实现，子类可重写
    }
}
