package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;

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

    /**
     * 尸水感染累积倍率（1.0 = 正常，0.4 = 高抗性，0.0 = 免疫）
     */
    default float getInfectionMultiplier() {
        return 1.0f;
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