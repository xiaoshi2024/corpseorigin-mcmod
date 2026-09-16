package xiaoshi2022.corpseorigin.client.limb;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端断肢状态缓存。
 * <p>
 * 数据来源就是服务端已经下发的 PlayerCorpseSyncS2C（整份 PLAYER_CORPSE tag），
 * 所以不需要额外的网络包。服务端每次状态变化才发一次，中间的一秒靠本地 tick 外推补间，
 * 避免按包频率一跳一跳地长。
 */
@Environment(EnvType.CLIENT)
public final class ClientLimbCache {

    /** 每部位进度常量：值 < 0 表示"断了但不会自愈" */
    public static final float PERMANENT = -1.0F;
    /** 完好 */
    public static final float INTACT = -2.0F;

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

    private ClientLimbCache() {
    }

    /** 一次渲染用的断肢视图 */
    public static final class Entry {
        private final CompoundTag tag;
        private final int mask;
        private final int[] remaining;
        private final int[] totals;
        private final long baseTick;

        private Entry(CompoundTag tag, int mask, int[] remaining, int[] totals, long baseTick) {
            this.tag = tag;
            this.mask = mask;
            this.remaining = remaining;
            this.totals = totals;
            this.baseTick = baseTick;
        }

        public int mask() {
            return mask;
        }

        /** 该部位是否断了 */
        public boolean isSevered(int slot) {
            return (mask & (1 << slot)) != 0;
        }

        /** 再生进度：0 = 刚断，1 = 长好；负数见 {@link #PERMANENT} / {@link #INTACT} */
        public float progress(int slot, float partialTick) {
            if (!isSevered(slot)) {
                return INTACT;
            }
            if (totals[slot] <= 0) {
                return PERMANENT;
            }
            float now = remaining[slot] - (currentTick() - baseTick) - partialTick;
            return Mth.clamp(1.0F - now / totals[slot], 0.0F, 1.0F);
        }
    }

    /**
     * 取该玩家的断肢状态；返回 null 表示按原版玩家渲染。
     * <p>
     * 只有服务端写了 limb_mask 的身体才会走断肢模型 —— 而服务端只对「黑小飞 + 尸兄」写这份数据，
     * 所以客户端不需要、也无法自行判断角色。
     */
    public static Entry get(AbstractClientPlayer player) {
        UUID uuid = player.getUUID();
        CorpseOriginClient.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        if (data == null || !data.isCorpse || data.isDisguised()) {
            CACHE.remove(uuid);
            return null;
        }

        CompoundTag tag = data.data;
        if (tag.getByte("limb_mask").isEmpty()) {
            CACHE.remove(uuid);
            return null;
        }

        int mask = tag.getByteOr("limb_mask", (byte) 0) & 0xF;
        int[] remaining = tag.getIntArray("limb_regrow_ticks").orElse(null);
        int[] totals = tag.getIntArray("limb_regrow_totals").orElse(null);
        if (mask == 0 || remaining == null || totals == null
                || remaining.length < LimbSlots.COUNT || totals.length < LimbSlots.COUNT) {
            CACHE.remove(uuid);
            return null;
        }

        Entry cached = CACHE.get(uuid);
        // 每次同步都是一个新 tag 实例，用身份比较判断有没有更新
        if (cached == null || cached.tag != tag) {
            cached = new Entry(tag, mask, remaining, totals, currentTick());
            CACHE.put(uuid, cached);
        }
        return cached;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static long currentTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }
}
