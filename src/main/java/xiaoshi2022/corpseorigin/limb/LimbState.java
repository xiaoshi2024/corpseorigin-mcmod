package xiaoshi2022.corpseorigin.limb;

import java.util.Arrays;

/**
 * 一个玩家的断肢状态快照（存进 PLAYER_CORPSE 附件，随尸兄数据一起同步）。
 *
 * @param mask        limb_mask，断掉的部位位掩码
 * @param regrowTicks 每个部位再生剩余 tick；{@link LimbSlots#REGROW_PERMANENT} = 不能自愈
 * @param totals      每个部位本次再生的总 tick（客户端按 剩余/总 插值）
 * @param cooldowns   每个部位"新生脆弱期"剩余 tick（期间再次被截断概率翻倍）
 */
public record LimbState(byte mask, int[] regrowTicks, int[] totals, int[] cooldowns) {

    public static final LimbState EMPTY = new LimbState(
            LimbSlots.NONE,
            new int[]{LimbSlots.REGROW_PERMANENT, LimbSlots.REGROW_PERMANENT,
                    LimbSlots.REGROW_PERMANENT, LimbSlots.REGROW_PERMANENT},
            new int[LimbSlots.COUNT],
            new int[LimbSlots.COUNT]);

    /** 可写的空状态（数组是独立副本） */
    public static LimbState empty() {
        return new LimbState(LimbSlots.NONE, EMPTY.regrowTicks().clone(),
                EMPTY.totals().clone(), EMPTY.cooldowns().clone());
    }

    public boolean hasSevered() {
        return mask != LimbSlots.NONE;
    }

    public boolean isSevered(int slot) {
        return LimbSlots.isSevered(mask, slot);
    }

    public boolean isRegrowing(int slot) {
        return LimbSlots.isRegrowing(mask, regrowTicks, slot);
    }

    public byte withSevered(int slot) {
        return LimbSlots.withSevered(mask, slot);
    }

    public byte withoutSevered(int slot) {
        return LimbSlots.withoutSevered(mask, slot);
    }

    @Override
    public String toString() {
        return "LimbState[mask=" + mask
                + ", regrow=" + Arrays.toString(regrowTicks)
                + ", totals=" + Arrays.toString(totals)
                + ", cooldowns=" + Arrays.toString(cooldowns) + "]";
    }
}
