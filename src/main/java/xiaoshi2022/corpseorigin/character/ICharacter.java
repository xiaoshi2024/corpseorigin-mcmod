package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.List;

/**
 * 角色接口 - 角色扮演系统的核心抽象
 */
public interface ICharacter {

    /** 角色唯一ID */
    String getId();

    /** 角色名称 */
    Component getName();

    /** 角色描述 */
    Component getDescription();

    /** 角色图标 */
    Identifier getIcon();

    /** 是否为被动角色（凡人） */
    boolean isPassive();

    /** 角色特性描述 */
    List<Component> getTraits();

    /** 角色拥有的技能（进化树用，默认无） */
    default List<ISkill> getSkills() {
        return List.of();
    }

    /**
     * 尸水感染累积倍率（1.0 = 正常，0.4 = 高抗性，0.0 = 免疫）
     */
    default float getInfectionMultiplier() {
        return 1.0f;
    }

    /**
     * 内力上限（0 = 无内力，不显示内力条）。
     * <p>
     * 尸兄原著中拥有内力的角色（白小飞、龙右、黑小飞等）返回正值，
     * 凡人和普通角色返回 0，不参与内力系统。
     */
    default int getMaxInnerPower() {
        return 0;
    }

    /** 服务端玩家获得角色时的回调 */
    default void onAcquire(Player player) {
        // 空实现，子类可重写
    }

    /** 服务端玩家失去角色时的回调 */
    default void onLose(Player player) {
        // 空实现，子类可重写
    }

    /** 创建角色图标 ID */
    static Identifier iconId(String path) {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/character/" + path + ".png");
    }
}