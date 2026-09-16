package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

/**
 * 再生策略：决定"谁长得多快、拿什么当代价"。
 * <p>
 * 默认注册两条，从优先到次优：
 * <ol>
 *   <li>{@link VampireRegenProfile} —— 黑小飞·吸血鬼血脉：不消化饱食度、再生更快</li>
 *   <li>{@link CorpseRegenProfile} —— 普通尸兄：按类型 / 进化等级，消耗饱食度</li>
 * </ol>
 * 要加新策略（比如"吸收生命精华"、"吃腐肉临时嫁接"）就实现本接口后
 * {@link LimbRegenProfiles#register} 一条。
 */
public interface LimbRegenProfile {

    String id();

    /** 越小越优先；默认 {@link CorpseRegenProfile} 是 100 */
    default int priority() {
        return 100;
    }

    /** 这套策略是否作用于该玩家 */
    boolean appliesTo(ServerPlayer player);

    /**
     * 该部位的再生总时长（tick）。
     * 返回 {@link LimbSlots#REGROW_PERMANENT} 表示"断了但不会自愈"（等道具）。
     */
    int regrowTicks(ServerPlayer player, PlayerCorpseComponent comp, int slot);

    /** 再生是否消耗饱食度（吸血鬼血脉为 false） */
    default boolean consumesHunger(ServerPlayer player) {
        return true;
    }

    /** 每消耗 1 格原版饥饿（= 5 点尸兄饥饿）对应的再生时长（tick）；{@link #consumesHunger} 为 false 时忽略 */
    default int hungerCostInterval(ServerPlayer player) {
        return 3 * 20;
    }

    /** 饥饿不足时是否允许继续长；返回 false 就暂停（不回退） */
    default boolean hungerGate(ServerPlayer player, PlayerCorpseComponent comp) {
        return true;
    }
}
