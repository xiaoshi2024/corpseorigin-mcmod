package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

/**
 * 普通尸兄的再生策略：按尸兄类型 / 进化等级定时长，消耗饱食度。
 * <p>
 * 原著依据："高级尸兄之下，几乎无法断肢重生……变异程度越高恢复力越出色"。
 */
public final class CorpseRegenProfile implements LimbRegenProfile {

    public static final String ID = "corpseorigin:corpse";

    /**
     * 再生总时长（tick）。
     * <p>
     * ⚠️ 这三档必须和 {@code corpse_player.animation.json} 里 {@code regrow_*} 的
     * {@code animation_length}（秒）保持一致 —— 动画是"播完即停在末帧"的，服务端如果比动画长，
     * 动画早就定格在"长好"那一帧、但 {@code limb_mask} 还没清，手臂就一直不回来。
     * 现在统一按 5 秒（100 tick）对齐那条动画。想给不同尸兄档位不同速度时，
     * 要么为每档单独做一条动画，要么改成按比例调速。
     */
    public static final int ELITE_TICKS = 5 * 20;
    /** 满级（进化 5 级） */
    public static final int MAX_LEVEL_TICKS = 5 * 20;
    /** 尸王（龙右不死髅体） */
    public static final int KING_TICKS = 5 * 20;

    /**
     * 饥饿低于该值（尸兄饥饿 10 = 原版 2 格）就停长。
     * 留 2 格是为了不让玩家在再生途中把自己饿死，但也不足以白嫖完整支手臂 ——
     * 想长完就得中途进食。
     */
    public static final int MIN_HUNGER = 10;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int priority() {
        return 100;
    }

    /** 兜底策略：对谁都成立 */
    @Override
    public boolean appliesTo(ServerPlayer player) {
        return true;
    }

    @Override
    public int regrowTicks(ServerPlayer player, PlayerCorpseComponent comp, int slot) {
        return baseTicks(comp);
    }

    /** 按尸兄类型 / 进化等级算基础时长；其他策略（例如吸血鬼血脉加速）可以复用 */
    public static int baseTicks(PlayerCorpseComponent comp) {
        if (comp.getCorpseType() == PlayerCorpseComponent.TYPE_KING) {
            return KING_TICKS;
        }
        if (comp.getEvolutionLevel() >= PlayerCorpseComponent.MAX_EVOLUTION_LEVEL) {
            return MAX_LEVEL_TICKS;
        }
        if (comp.getCorpseType() >= PlayerCorpseComponent.TYPE_ELITE) {
            return ELITE_TICKS;
        }
        return LimbSlots.REGROW_PERMANENT;   // 普通尸兄不能自愈，只能靠道具
    }

    @Override
    public boolean hungerGate(ServerPlayer player, PlayerCorpseComponent comp) {
        return comp.getHunger() >= MIN_HUNGER;
    }
}
